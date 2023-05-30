(ns cljest.core-test
  (:require [cljest.core :refer [describe is it]]
            [cljest.matchers :as m]
            [cyrik.cljs-macroexpand :refer [cljs-macroexpand-all] :rename {cljs-macroexpand-all macroexpand-all}]
            [malli.core :as malli]))

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
    (let [expanded (macroexpand-all '(is (= 3 (+ 1 2))))]

      (is (= (macroexpand-all '(. (js/expect #(do (= 3 (+ 1 2)))) -cljest__is)) (nth expanded 1)))

      ;; While it's a bit clunky to validate with `:tuple`, we want to assert that we have a symbol
      ;; (call), nil, and then a sequence, which is the formatter function. I tried to use `match` but
      ;; couldn't get it to work.
      ;;
      ;; Suggestions for improving this assertion are welcome!
      (is (malli/validate [:tuple symbol? nil? seq?] (into [] (nth expanded 2))))))

  (it "should create an expect call when expanded with a primitive value"
    (let [expanded (macroexpand-all '(is true))]
      (is (= (macroexpand-all '(. (js/expect #(do true)) -cljest__is)) (nth expanded 1)))
      (is (malli/validate [:tuple symbol? nil? seq?] (into [] (nth expanded 2))))))

  (it "should create only one expect call if called with a matcher"
    (is (= (macroexpand-all '(is (m/visible? (h.dom/get-by :text "hello"))))
           (macroexpand-all '(.. (js/expect (h.dom/get-by :text "hello")) -toBeVisible (call nil))))))

  (it "should correctly negate matchers"
    (is (= (macroexpand-all '(is (not (m/visible? (h.dom/get-by :text "hello")))))
           (macroexpand-all '(.. (js/expect (h.dom/get-by :text "hello")) -not -toBeVisible (call nil))))))

  (it "should pass arguments to matchers"
    (is (= (macroexpand-all '(is (m/has-text-content? (h.dom/get-by :text "hello") "world" {:normalizeWhitespace true})))
           (macroexpand-all '(.. (js/expect (h.dom/get-by :text "hello")) -toHaveTextContent (call nil "world" {:normalizeWhitespace true})))))))
