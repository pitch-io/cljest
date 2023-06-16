(ns cljest.deploy
  (:require [cljest.build :as build]
            [deps-deploy.deps-deploy :as deps-deploy]))

(defn main
  "Publishes the library. By default publishes locally, and publishes to
  Clojars if called with `:clojars? true`."
  [{:keys [clojars?]}]
  (prn clojars?)
  (let [installer (if clojars? :remote :local)]
    (deps-deploy/deploy {:artifact (build/jar-path)
                         :pom-file (build/pom-path)
                         :sign-release? false
                         :installer installer})))
