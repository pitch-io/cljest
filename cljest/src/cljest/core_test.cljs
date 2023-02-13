(ns cljest.core-test
  (:require [cljest.core :refer [describe is it]]))

(describe "is"
  (it "should support non-list forms (primitives)"
    (let [three 3]
      (is true)
      (is :kw)
      (is three)))

  (it "should support list forms (non-primitives)"
    (let [false?! false]
      (is (not false?!))
      (is (= false?! false))
      (is (= 3 (+ 1 2)))))

  (it "can be wrapped in try-catch"
    (let [ex (atom nil)]
      (try
        (is false)
        (catch :default e (reset! ex e)))
      (is @ex))))
