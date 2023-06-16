#!/usr/bin/env bb

(require '[babashka.fs :as fs]
         '[cheshire.core :as json]
         '[clojure.edn :as edn])

(def cljest-version (-> (fs/file "./cljest/build.edn")
                        slurp
                        edn/read-string
                        :version))
(def jest-preset-cljest-version (-> (fs/file "./jest-preset-cljest/package.json")
                                    slurp
                                    (json/parse-string true)
                                    :version))

;; TODO: properly handle our semantic versioning expectations. If we release an alpha
;;       package (e.g. 1.2.0-alpha1) and the other package is 1.1.1, this shouldn't fail.
(when-not (= cljest-version jest-preset-cljest-version)
  (prn (format "cljest (%s) and jest-preset-cljest (%s) versions do not match."
               cljest-version
               jest-preset-cljest-version))
  (System/exit 1))
