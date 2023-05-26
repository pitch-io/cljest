(ns examples.async-test
  (:require [cljest.core :refer [is it spy]]
            [cljest.helpers.core :as h]
            [cljest.matchers :as m]))

(defn ^:private fire-async
  "Calls given `cb` asynchronously (on the next tick)."
  [cb]
  (js/process.nextTick cb))

(it "fire-async calls the cb asynchronously"
  (let [cb (spy)]
    (fire-async cb)
    (is (not (m/called? cb)))

    (h/async
     (await (js/Promise. (fn [res] (js/setTimeout res))))
     (is (m/called? cb)))))
