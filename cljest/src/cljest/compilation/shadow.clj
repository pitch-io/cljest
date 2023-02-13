(ns cljest.compilation.shadow
  (:require [cljest.compilation.config :as config]
            [cljest.compilation.fs :as fs]
            [clojure.core.async :as as]
            [shadow.build.classpath]
            [shadow.cljs.devtools.api :as devtools.api]
            [shadow.cljs.devtools.config :as devtools.config]
            [shadow.cljs.devtools.server :as devtools.server]
            [shadow.cljs.devtools.server.fs-watch :as devtools.server.fs-watch]
            [shadow.cljs.devtools.server.reload-classpath :as devtools.server.reload-classpath]
            [shadow.cljs.devtools.server.runtime :as devtools.server.runtime]
            [shadow.cljs.devtools.server.system-bus :as devtools.server.system-bus]
            [shadow.cljs.model]
            [shadow.cljs.util]))

(def ^:private build-target ::jest)

(defonce ^:private shadow-config
   ; See https://github.com/thheller/shadow-cljs/blob/650d78f2a7d81f33cb2590e142ddcbcbd756d781/src/main/shadow/cljs/devtools/server/fs_watch.clj#L34
   ; While we stop polling right away when starting the server, we don't want to poll even once, so we want the value to be arbitrarily high
   ; and make sure it never polls the watchers.
  (atom (merge devtools.config/default-config {:fs-watch {:loop-wait Integer/MAX_VALUE}})))

(defn install-config!
  "Alters `shadow.cljs.devtools.config/load-cljs-edn` to use our internal config atom rather than a file."
  []
  ;; Some functions either do not allow passing the config or just call `load-cljs-edn`, which means we have no choice but to
  ;; forcibly override the function to return the latest value of our internal config atom.
  (alter-var-root (var devtools.config/load-cljs-edn) (fn [& _] (fn [& _] @shadow-config))))

(defn ^:private get-runtime-instance!
  []
  (devtools.server.runtime/get-instance!))

(defn start-server!
  []
  (devtools.server/start! @shadow-config)
  (devtools.server.fs-watch/stop (:cljs-watch (get-runtime-instance!))))

(defn- get-system-bus
  "Gets the system bus from the runtime instance. Used for pub/sub events that happen during the lifecycle of
  the server."
  []
  (get @devtools.server.runtime/instance-ref :system-bus))

(defn- sub-to-system-bus-topic
  "Subscribe to `topic` from the system bus using `chan`."
  [chan topic]
  (devtools.server.system-bus/sub (get-system-bus) topic chan))

(defn- unsub-to-system-bus-topic
  "Unsubscribe to `topic` from the system bus using `chan`."
  [chan topic]
  (devtools.server.system-bus/unsub (get-system-bus) topic chan))

(defn- get-1st-msg-on-chan
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

(defn- publish-file-changes!
  "For all directories being watched by the runtime instance, publish any changes in those dirs. If there are changes,
  waits for all processing done as a result of the change event.

  This is done in this way, rather than watching normally, primarily because Jest includes a file watcher and we utilize
  its watching capabilities to call the API and we don't want to also use shadow's. However, due to the way shadow works,
  we do need to check what files have changed and let the worker(s) know so that when the next compilation happens, the
  worker actually attempts to compile the file. Without this the compilation would not be controllable and consistent."
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
      (let [changes (fs/get-latest-changes)]
        (when (seq changes)
          (let [fs-watch-chan (as/chan 1)]
            ;; This event is listened to inside of `devtools.server.reload-classpath` as well, and is what triggers
            ;; the call to `process-updates`.
            (devtools.server.system-bus/publish! (get-system-bus) :shadow.cljs.model/cljs-watch {:updates changes})

            (get-1st-msg-on-chan fs-watch-chan ::updates-processed)))))))

(defn get-compilation-result
  "Gets the next successful or failure compilation result for `target`."
  []
  (let [compilation-chan (as/chan 1 (filter #(contains? #{:build-complete :build-failure} (:type %))))]

    (get-1st-msg-on-chan compilation-chan [:shadow.cljs.model/worker-output build-target])))

(defn generate-build! []
  (let [test-nses (fs/get-test-files-from-src-dirs)
        {:keys [preloads-ns]} (config/get-config!)
        build-definition {:build-id build-target
                          :target :npm-module
                          :build-options {:greedy true :dynamic-resolve true}
                          :compiler-options {:output-feature-set :es8
                                             ; We want to call functions using `.call` so that spying works as expected
                                             :static-fns false
                                             :infer-externs false}
                          :output-dir ".jest"
                          :devtools {:enabled false}
                          :entries (into [] (conj test-nses preloads-ns))}]
    (swap! shadow-config assoc-in [:builds build-target] build-definition)))

(defn publish-and-compile!
  []
  (publish-file-changes!)
  (devtools.api/watch-compile! build-target))

(defn compile!
  []
  (devtools.api/compile build-target))

(defn start-watching
  []
  (devtools.api/watch build-target {:autobuild false}))
