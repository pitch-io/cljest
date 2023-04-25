(ns cljest.compilation.shadow
  (:require [cljest.compilation.config :as config]
            [cljest.compilation.fs :as fs]
            [cljest.compilation.utils :as utils]
            [clojure.core.async :as as]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [shadow.build :as build]
            [shadow.build.classpath]
            [shadow.build.modules :as build.modules]
            [shadow.cljs.devtools.api :as devtools.api]
            [shadow.cljs.devtools.config :as devtools.config]
            [shadow.cljs.devtools.server :as devtools.server]
            [shadow.cljs.devtools.server.fs-watch :as devtools.server.fs-watch]
            [shadow.cljs.devtools.server.reload-classpath :as devtools.server.reload-classpath]
            [shadow.cljs.devtools.server.runtime :as devtools.server.runtime]
            [shadow.cljs.devtools.server.supervisor :as devtools.server.supervisor]
            [shadow.cljs.devtools.server.system-bus :as devtools.server.system-bus]
            [shadow.cljs.devtools.server.worker :as devtools.server.worker]
            [shadow.cljs.devtools.server.worker.impl :as devtools.server.worker.impl]
            [shadow.cljs.model]
            [shadow.cljs.util]
            [taoensso.timbre :as log]))

(def ^:private build-target ::jest)

(defonce ^:private shadow-config
   ; See https://github.com/thheller/shadow-cljs/blob/650d78f2a7d81f33cb2590e142ddcbcbd756d781/src/main/shadow/cljs/devtools/server/fs_watch.clj#L34
   ; While we stop polling right away when starting the server, we don't want to poll even once, so we want the value to be arbitrarily high
   ; and make sure it never polls the watchers.
  (atom (merge devtools.config/default-config {:fs-watch {:loop-wait Integer/MAX_VALUE}
                                               :cache-dir ".jest/shadow"})))

(defn install-config!
  "Alters `shadow.cljs.devtools.config/load-cljs-edn` to use our internal config atom rather than a file.

  Some functions either do not allow passing the config or just call `load-cljs-edn`, which means we have no choice but to
  forcibly override the function to return the latest value of our internal config atom."
  []
  (alter-var-root (var devtools.config/load-cljs-edn) (fn [& _] (fn [& _] @shadow-config))))

(defn ^:private get-runtime-instance!
  []
  (devtools.server.runtime/get-instance!))

(defn start-server!
  "Starts the shadow dev server, and stops the automatically set up file watchers."
  []
  (devtools.server/start! @shadow-config)
  (devtools.server.fs-watch/stop (:cljs-watch (get-runtime-instance!))))

(defn ^:private get-system-bus
  "Gets the system bus from the runtime instance. Used for pub/sub events that happen during the lifecycle of
  the server."
  []
  (get @devtools.server.runtime/instance-ref :system-bus))

(defn ^:private sub-to-system-bus-topic
  "Subscribe to `topic` from the system bus using `chan`."
  [chan topic]
  (devtools.server.system-bus/sub (get-system-bus) topic chan))

(defn ^:private unsub-to-system-bus-topic
  "Unsubscribe to `topic` from the system bus using `chan`."
  [chan topic]
  (devtools.server.system-bus/unsub (get-system-bus) topic chan))

(defn ^:private get-1st-msg-on-chan
  "Subscribes to `topic` using `chan`, gets the first message that the channel listens for,
  and then unsubscribes, returning message. If `timeout-ms` is provided will wait up to that
  number of ms for a message or return `nil`.

  The channel is configurable here as it may not always simply listen for any event, instead
  possibly including a transducer that filters for specific messages."
  [chan topic & [timeout-ms]]
  (let [msg-chan (as/go
                   (if timeout-ms
                     (as/alts! [(as/timeout timeout-ms) chan])
                     (as/<! chan)))]

    (sub-to-system-bus-topic chan topic)

    (let [msg (as/<!! msg-chan)]
      (unsub-to-system-bus-topic chan topic)

      msg)))

(defn ^:private get-build-worker
  []
  (let [{:keys [supervisor]} (devtools.server.runtime/get-instance!)]
    (devtools.server.supervisor/get-worker supervisor build-target)))

(defn ^:private sync-worker!
  []
  (devtools.server.worker/sync! (get-build-worker)))

(defmethod devtools.server.worker.impl/do-proc-control :update-build-entries
  [worker-state {:keys [reply-to]}]

  (let [entries (get-in @shadow-config [:builds build-target :entries])
        result (-> worker-state
                   (update :build-state assoc-in [::build/config :entries] entries)
                   (update :build-state build.modules/analyze))]
    (as/>!! reply-to :done)

    result))

(defn ^:private update-build-entries!
  "Called when the shadow config is updated to update the worker's build entries."
  [{:keys [proc-control]}]
  (let [reply-chan (as/chan)]
    (as/>!! proc-control {:type :update-build-entries :reply-to reply-chan})
    (as/<!! reply-chan)))

(defn ^:private event-for-test-file?
  [event]
  (let [{filename :name ext :ext} event
        ns (shadow.cljs.util/filename->ns filename)]
    (and (= "cljs" ext) (utils/test-ns? ns))))

(defn ^:private added-test-event?
  [event]
  (let [{event-kind :event} event]
    (and (event-for-test-file? event) (= :new event-kind))))

(defn ^:private removed-test-event?
  [event]
  (let [{event-kind :event} event]
    (and (event-for-test-file? event) (= :del event-kind))))

(defn ^:private update-build-namespaces!
  [events]
  (let [entries-set (into #{} (get-in @shadow-config [:builds build-target :entries]))
        new-entries (->> events
                         (reduce (fn [entries {filename :name :as event}]
                                   (let [ns (shadow.cljs.util/filename->ns filename)]
                                     (cond
                                       (added-test-event? event)
                                       (conj entries ns)

                                       (removed-test-event? event)
                                       (set/difference entries #{ns})

                                       :else
                                       entries)))
                                 entries-set)
                         (into []))]
    (swap! shadow-config assoc-in [:builds build-target :entries] new-entries)

    (let [new-config (get-in @shadow-config [:builds build-target])]
      (devtools.server.system-bus/publish! (get-system-bus) [:shadow.cljs.model/config-watch build-target] {:config new-config}))
    (sync-worker!)

    (update-build-entries! (get-build-worker))

    (sync-worker!)))

(defn ^:private publish-file-changes!
  "For all directories being watched, publish any changes in those dirs. If there are changes, waits for all processing
  done as a result of the change event.

  This is done in this way, rather than watching normally in shadow, primarily because Jest includes a file watcher and
  we utilize its watching capabilities to call the API and don't want to use shadow's. However, due to the way shadow works,
  we do need to check what files have changed and let the worker(s) know so that when the next compilation happens, the
  worker actually attempts to compile the right file(s). Without this the compilation would not be controllable and consistent
  from the perspective of the Jest process."
  []
  ;; Inside of `shadow-cljs`, the actual code that handles emitting when something actually changes lives in the function
  ;; `devtools.server.reload-classpath/process-updates`. This function performs some calculations and, if it detects any
  ;; changes, emits a `shadow.cljs.model/resource-update` message on the system bus. However, if it detects no changes it
  ;; emits no message on the system bus. To ensure we _always_ get a message we redefine it here, call the original, and once
  ;; the original returns, emit a custom ::updates-processed message, which is listened to below.
  (let [orig-process-updates devtools.server.reload-classpath/process-updates
        new-process-updates (fn [& args]
                              (let [result (apply orig-process-updates args)]
                                (devtools.server.system-bus/publish! (get-system-bus) ::updates-processed {})
                                result))]
    (with-redefs [devtools.server.reload-classpath/process-updates new-process-updates]
      (let [changes (fs/get-latest-changes!)]
        (when (seq changes)
          (let [fs-watch-chan (as/chan 1)]
            (update-build-namespaces! changes)
            ;; This event is listened to inside of `devtools.server.reload-classpath` as well, and is what triggers
            ;; the call to `process-updates`.
            (devtools.server.system-bus/publish! (get-system-bus) :shadow.cljs.model/cljs-watch {:updates changes})

            (get-1st-msg-on-chan fs-watch-chan ::updates-processed)))))))

(defn get-compilation-result
  "Gets the next :build-complete or :build-failure result for compilation of ::jest."
  []
  (let [compilation-chan (as/chan 1 (filter #(contains? #{:build-complete :build-failure} (:type %))))]

    (get-1st-msg-on-chan compilation-chan [:shadow.cljs.model/worker-output build-target])))

(defn get-build-directory
  "Returns the absolute path of the build directory."
  []
  (let [dir (io/file ".jest")]
    (.getCanonicalPath dir)))

(defn generate-build!
  "Generates an initial build based on the test namespaces, setup namespace, and compiler options."
  []
  (let [test-nses (fs/get-test-files-from-src-dirs)
        {:keys [setup-ns compiler-options]} (config/get-config!)
        build-definition {:build-id build-target
                          :target :npm-module
                          :build-options {:greedy true :dynamic-resolve true}
                          :compiler-options (merge {:output-feature-set :es8
                                                   ; We want to call functions using `.call` so that spying works as expected
                                                    :static-fns false
                                                    :infer-externs false}
                                                   compiler-options)
                          :output-dir ".jest"
                          :devtools {:enabled false}
                          :entries (into [] (conj test-nses setup-ns))}]
    (swap! shadow-config assoc-in [:builds build-target] build-definition)))

(defn publish-and-compile!
  "Publishes and watched directory fs changes and triggers a compilation. Blocks until compilation is complete."
  []
  (publish-file-changes!)
  (devtools.api/watch-compile! build-target))

(defn compile!
  "Triggers a compilation."
  []
  (devtools.api/compile build-target))

(defn start-watching
  []
  (devtools.api/watch build-target {:autobuild false}))
