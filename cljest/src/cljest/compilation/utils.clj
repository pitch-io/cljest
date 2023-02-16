(ns cljest.compilation.utils
  (:require [cljest.compilation.config :as config]
            [clojure.string :as str]))

(defn test-ns?
  [ns]
  (let [{:keys [ns-suffixes]} (config/get-config!)]
    (some (fn [suffix]
            (str/ends-with? (str ns) (str suffix))) ns-suffixes)))
