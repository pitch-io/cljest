(ns cljest.server
  (:require [cheshire.core :as cheshire]
            [clojure.core.async :as as]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [shadow.build.classpath]
            [shadow.cljs.devtools.api :as devtools.api]
            [shadow.cljs.devtools.config :as devtools.config]
            [shadow.cljs.devtools.server :as devtools.server]
            [shadow.cljs.devtools.server.fs-watch :as devtools.server.fs-watch]
            [shadow.cljs.devtools.server.reload-classpath :as devtools.server.reload-classpath]
            [shadow.cljs.devtools.server.runtime :as devtools.server.runtime]
            [shadow.cljs.devtools.server.system-bus :as devtools.server.system-bus]
            [shadow.cljs.model]
            [taoensso.timbre :as log]))

(defonce ^{:doc "The current build status, used by the /build-status endpoint. The following statuses may be in the atom:

  - :unknown - When the initial `devtools.api/watch` hasn't completed.
  - :success - If the initial compilation or any subsequent incremental compilations via `watch-compile!` succeed.
  - :initial-failure - If the initial compilation failed.
  - :failure - If an incremental compilation fails.

  Additionally, if the :status is :failure/:initial-failure, two additional keys will be in the map:

  - :error - the specific error that caused the compilation to fail.
  - :raw-status - the raw `type` that comes from the `[:shadow.cljs.model/worker-output target]` topic event."}
  !build-status
  (atom {:status :unknown}))

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

(defn- get-latest-watched-changes!
  "Gets the latest changes for the directories that are watched by the shadow server instance. Times out after 500ms
  with `nil` if there are no changes."
  []
  (let [watch-dirs (get-in @devtools.server.runtime/instance-ref [:cljs-watch :watch-dirs])
        stop-chan (as/chan)

        ; This kind of weird/roundabout way of creating a polling channel is necessary because we want to both poll
        ; for the latest changes in a channel (so that it can be timed out using `alts!!`) as well as kill it. Simply
        ; closing a channel does not actually kill it, and so if we didn't include the `stop-chan` inside of the `alt!`
        ; in the loop, it would loop forever, even after calling `close!`.
        ;
        ; There are ways to make it work without passing all of the channels around, but it would move some of the
        ; logic inline and inside of `if` expressions (as the underlying polling happens synchronously and we want to check
        ; if the result is empty), and moving this logic inline which would arguably make it harder to understand.
        watch-loop (as/go-loop []
                     (let [changes (->> watch-dirs
                                        (mapcat devtools.server.fs-watch/poll-changes)
                                        (into []))]
                       (if (seq changes)
                         changes
                         (as/alt!
                           stop-chan nil
                           (as/timeout 10) (recur)))))
        [changes] (as/alts!! [(as/timeout 500) watch-loop])]

    (when-not changes
      (as/put! stop-chan true))

    changes))

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
      (let [changes (get-latest-watched-changes!)]
        (when (seq changes)
          (let [fs-watch-chan (as/chan 1)]
            ;; This event is listened to inside of `devtools.server.reload-classpath` as well, and is what triggers
            ;; the call to `process-updates`.
            (devtools.server.system-bus/publish! (get-system-bus) :shadow.cljs.model/cljs-watch {:updates changes})

            (get-1st-msg-on-chan fs-watch-chan ::updates-processed)))))))

(defn- get-compilation-result
  "Gets the next successful or failure compilation result for `target`."
  [target]
  (let [compilation-chan (as/chan 1 (filter #(contains? #{:build-complete :build-failure} (:type %))))]

    (get-1st-msg-on-chan compilation-chan [:shadow.cljs.model/worker-output target])))

(defn- compile-and-update-build-status!
  "Compiles `target` and updates the `!build-status` atom with the latest state
  from the server. Returns the updated state."
  [target]

  ; If the status is unknown, the initial watch is still running, so we don't need to attempt to look for
  ; file changes or compile and can simply wait until the compilation result returns
  (when-not (= :unknown (:status @!build-status))
    ; Since workers don't actually perform an incremental compilation until they see a file system change,
    ; we need to poll for any changes and publish them so the compilation that happens below will pick up
    ; the changes.
    (publish-file-changes!)

    (as/go
      (devtools.api/watch-compile! target)))

  (let [{:keys [type report]} (get-compilation-result target)]
    (cond
      (= :build-complete type)
      (swap! !build-status merge {:status :success
                                  :raw-status type
                                  :error nil})

      ; If `type` isn't `:build-complete`, it must be `:build-failure` due to the channel used in
      ; `get-compilation-result`. The only thing we're checking here is if the current status is
      ; unknown/initial-failure.
      (contains? #{:unknown :initial-failure} (:status @!build-status))
      (swap! !build-status merge {:status :initial-failure
                                  :raw-status type
                                  :error report})

      :else
      (swap! !build-status merge {:status :failure
                                  :raw-status type
                                  :error report}))

    @!build-status))

(defn- handler [target]
  (fn handler-inner [{:keys [request-method uri]}]
    (cond
      (and (= :get request-method) (= "/compile" uri))
      (let [{:keys [status]} (compile-and-update-build-status! target)]
        (if (= :success status)
          {:status 204}
          {:status 418
           :headers {"Content-Type" "application/json"}
           :body (-> @!build-status
                     (cheshire/generate-string))}))

      (and (= :get request-method) (= "/build-status" uri))
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (-> @!build-status
                 (cheshire/generate-string))}

      :else
      {:status 404})))

(defn -main [raw-port raw-target-name]
  (let [target (keyword raw-target-name)
        port (Integer/parseInt raw-port)
        ; See https://github.com/thheller/shadow-cljs/blob/650d78f2a7d81f33cb2590e142ddcbcbd756d781/src/main/shadow/cljs/devtools/server/fs_watch.clj#L34
        ; We want to disable shadow's watching and only poll for updates when a `/compile` API call comes
        ; in. We can't disable it programmatically but we can give it an arbitrarily large integer.
        config (merge (devtools.config/load-cljs-edn) {:fs-watch {:loop-wait Integer/MAX_VALUE}})]

    (devtools.server/start! config)
    (log/infof "Starting Jest compilation server for %s" target)
    (log/infof "HTTP server at http://localhost:%s for target %s" port target)

    ; Run both async to not block, and run both in general to update the build status after
    ; the initial watch.
    ;
    ; In regular operation with Jest, this probably wouldn't happen because it would call
    ; `/compile` immediately (and before watching finishes), but in general, without calling
    ; `compile-and-update-build-status!` before the initial watch finishes, the status would
    ; never update from `:unknown` and any subsequent `/compile` API call would hang.
    (as/go (compile-and-update-build-status! target))
    (as/go (devtools.api/watch target {:autobuild false}))

    (run-jetty
     (-> (handler target)
         (wrap-defaults site-defaults)
         (wrap-resource "")
         (wrap-params))
     {:port port
      :join? false})))
