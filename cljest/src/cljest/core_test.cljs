(ns cljest.core-test
  (:require [cljest.core :refer [describe is it]]
            [cljest.helpers.dom :as h.dom]
            [cljest.matchers :as m]
            [cyrik.cljs-macroexpand :refer [cljs-macroexpand-all] :rename {cljs-macroexpand-all macroexpand-all}]))

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
      (is @ex)))

  (it "should create an `is` expect call when expanded with a complex value"
    ;; Because the third value in the sequence is a function that we don't really care about, we only really
    ;; want to assert against the first two arguments. It's a bit cumbersome but I couldn't find a better way
    ;; to do this... If someone sees this and knows a better way to do it, please create a PR!
    (let [expanded (macroexpand-all '(is (= 3 (+ 1 2))))]
      (is (= 'cljest.core/is-matcher (first expanded)))
      (is (= (macroexpand-all '#(do (= 3 (+ 1 2)))) (second expanded)))))

  (it "should create an expect call when expanded with a primitive value"
    (let [expanded (macroexpand-all '(is true))]
      (is (= 'cljest.core/is-matcher (first expanded)))
      (is (= (macroexpand-all '#(do true)) (second expanded)))))

  (it "should create only one expect call if called with a matcher"
    (is (= (macroexpand-all '(is (m/visible? (h.dom/get-by :text "hello"))))
           (macroexpand-all '(.. (js/expect (h.dom/get-by :text "hello")) -toBeVisible (call nil))))))

  (it "should correctly negate matchers"
    (is (= (macroexpand-all '(is (not (m/visible? (h.dom/get-by :text "hello")))))
           (macroexpand-all '(.. (js/expect (h.dom/get-by :text "hello")) -not -toBeVisible (call nil))))))

  (it "should pass arguments to matchers"
    (is (= (macroexpand-all '(is (m/has-text-content? (h.dom/get-by :text "hello") "world" {:normalizeWhitespace true})))
           (macroexpand-all '(.. (js/expect (h.dom/get-by :text "hello")) -toHaveTextContent (call nil "world" {:normalizeWhitespace true})))))))
