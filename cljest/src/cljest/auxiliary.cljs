(ns cljest.auxiliary
  (:require [lambdaisland.deep-diff2 :as deep-diff2]))

(defn generate-diff
  "Generate a pretty diff of the two given values.

  Used by the formatters."
  [a b]
  (-> (deep-diff2/diff a b)
      deep-diff2/pretty-print
      with-out-str))
