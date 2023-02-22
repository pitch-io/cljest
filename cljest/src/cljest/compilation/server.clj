(ns cljest.compilation.server
  (:require [cheshire.core :as cheshire]
            [cljest.compilation.config :as config]
            [cljest.compilation.fs :as fs]
            [cljest.compilation.shadow :as shadow]
            [clojure.core.async :as as]
            [clojure.string :as str]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [shadow.cljs.util]
            [taoensso.timbre :as log]))

;; Jetty announces some debug information when it's imported, so to avoid this we require after telling it not
;; to announce.
;; Thanks to https://github.com/active-group/active-logger#jetty.
(.setProperty (org.eclipse.jetty.util.log.Log/getProperties) "org.eclipse.jetty.util.log.announce" "false")

(require '[ring.adapter.jetty :refer [run-jetty]])

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

(defn ^:private compile-and-update-build-status!
  "Compiles `::jest` and updates the `!build-status` atom with the latest state
  from the server. Returns the updated state."
  []

  ; If the status is unknown, the initial watch is still running, so we don't need to attempt to look for
  ; file changes or compile and can simply wait until the compilation result returns
  (when-not (= :unknown (:status @!build-status))
    ; Since workers don't actually perform an incremental compilation until they see a file system change,
    ; we need to poll for any changes and publish them so the compilation that happens below will pick up
    ; the changes.
    (as/go (shadow/publish-and-compile!)))

  (let [{:keys [type report]} (shadow/get-compilation-result)]
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

(defn ^:private ns->built-ns-path
  [ns]
  (let [build-dir (shadow/get-build-directory)
        relative-path (munge (name ns))]
    (str build-dir "/" relative-path ".js")))

(defn ^:private json-response
  ([body]
   (json-response 200 body))

  ([status body]
   {:status status
    :headers {"Content-Type" "application/json"}
    :body (cheshire/generate-string body)}))

(defn ^:private ns-successfully-compiled?
  [ns]
  (let [{:keys [status error]} @!build-status]
    (case status
      :unknown false
      :initial-failure false
      :success true
      :failure (str/includes? error (name ns)))))

(defn ^:private handle-setup-file
  "By the time /setup-file is called, compilation should be done, so we can trust the build status."
  []
  (let [{:keys [setup-ns]} (config/get-config!)]
    (cond
      (= :success (:status @!build-status))
      (json-response {:path (ns->built-ns-path setup-ns)})

      (= :initial-failure (:status @!build-status))
      (json-response 422 @!build-status)

      (and (= :failure (:status @!build-status)) (ns-successfully-compiled? setup-ns))
      (json-response {:path (ns->built-ns-path setup-ns)})

      (and (= :failure (:status @!build-status)) (not (ns-successfully-compiled? setup-ns)))
      (json-response 422 @!build-status)

      :else
      (json-response 500 {:error "Compilation is in an unknown state"}))))

(defn ^:private handle-compile
  []
  (let [{:keys [status]} (compile-and-update-build-status!)]
    (if (= :success status)
      {:status 204}
      {:status 418
       :headers {"Content-Type" "application/json"}
       :body (-> @!build-status
                 (cheshire/generate-string))})))

(defn ^:private handle-build-status
  []
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (-> @!build-status
             (cheshire/generate-string))})

(defn ^:private handler [{:keys [request-method uri]}]
  (cond
    (and (= :get request-method) (= "/compile" uri))
    (handle-compile)

    (and (= :get request-method) (= "/build-status" uri))
    (handle-build-status)

    (and (= :get request-method) (= "/setup-file" uri))
    (handle-setup-file)

    :else
    {:status 404}))

(defn start-server!
  "Starts the Jest compilation server on the port in cljest.edn."
  []
  (shadow/start-server!)
  (fs/setup-watchers!)

  (let [{:keys [port]} (config/get-config!)]
    (log/infof "Starting Jest compilation server")
    (log/infof "HTTP server at http://localhost:%s" port)

    ; Run both async to not block, and run both in general to update the build status after
    ; the initial watch.
    ;
    ; In regular operation with Jest, this probably wouldn't happen because it would call
    ; `/compile` immediately (and before watching finishes), but in general, without calling
    ; `compile-and-update-build-status!` before the initial watch finishes, the status would
    ; never update from `:unknown` and any subsequent `/compile` API call would hang.
    (as/go (compile-and-update-build-status!))
    (as/go (shadow/start-watching))

    (run-jetty
     (-> handler
         (wrap-defaults site-defaults)
         (wrap-resource "")
         (wrap-params))
     {:port port
      :join? false})))
