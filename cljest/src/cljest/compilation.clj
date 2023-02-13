(ns cljest.compilation
  (:refer-clojure :exclude [compile])
  (:require [cljest.compilation.config :as config]
            [cljest.compilation.server :as server]
            [cljest.compilation.shadow :as shadow]
            [taoensso.timbre :as log]))

(defn ^:private setup!
  "Performs setup that is shared between watch and compile mode.

  Specifically, it attempts to:

  - Load and coerce the jest-config.edn file
  - Installs the shadow-cljs config override
  - Generates the internal shadow-cljs build"
  []
  (try
    (config/get-config!)

    (catch Exception e
      (print (ex-message e))
      (println)

      (System/exit 1)))

  (shadow/install-config!)
  (shadow/generate-build!))

(defn watch
  "Runs cljest in watch mode, which starts a server and compiles test files on demand."
  [{:keys [port]}]
  (setup!)
  (server/start-server! port))

(defn compile
  [_]
  (setup!)
  (shadow/compile!))

(defn -main
  "An alias for watch mode."
  [raw-port]
  (let [port (Integer/parseInt raw-port)]
    (watch {:port port})))
