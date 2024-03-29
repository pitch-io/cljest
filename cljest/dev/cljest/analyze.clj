(ns cljest.analyze
  (:require cljdoc-analyzer.cljdoc-main
            [cljest.build :as build]))

(defn main
  "Runs a `cljdoc-analyzer` analysis. Requires the JAR and POM to be built
  (the `cljest.build/main` function needs to be run first)."
  [& _]
  (let [build-config (build/get-build-config)
        analysis-config {:version (:version build-config)
                         :project (str (:lib build-config))
                         :jarpath (build/jar-path)
                         :pompath (build/pom-path)}]
    (cljdoc-analyzer.cljdoc-main/-main (str analysis-config))))
