(ns cljest.compilation
  (:refer-clojure :exclude [compile])
  (:require [cljest.compilation.config :as config]
            [cljest.compilation.server :as server]
            [cljest.compilation.shadow :as shadow]))

;; TODO:
;; - Expose a preloads check endpoint, to check if the preloads NS is ready or not

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
  [{}]
  (setup!)
  (server/start-server!))

(defn compile
  [_]
  (setup!)
  (shadow/compile!))

(defn -main
  "An alias for watch mode."
  []
  (watch {}))
