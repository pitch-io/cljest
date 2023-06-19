(ns cljest.helpers.core-test
  (:require [cljest.core :refer [describe is it spy]]
            [cljest.helpers.core :as h]
            [cljest.matchers :as m]))

(defn ^:private next-macrotask+
  []
  (js/Promise. (fn [res _] (js/setTimeout res))))

(describe "with-mocks"
  (defn ^:private cool-fn
    [x y]
    (* x y))

  (it "works"
    (is (= 50 (cool-fn 5 10)))

    (h/with-mocks [cool-fn #(+ %1 %2)]
      (is (= 15 (cool-fn 5 10))))))

(describe "setup-mocks"
  (def ^:private something-stateful
    (let [counter (atom 0)]
      (fn []
        (swap! counter inc)
        @counter)))

  (def ^:private something-else-stateful
    (let [counter (atom 0)]
      (fn []
        (swap! counter dec)
        @counter)))

  ;; The presence of this function validates mocking the symbol
  ;; doesn't cause the function to be called. See https://github.com/pitch-io/cljest/issues/44
  ;; The same could be done with a primitive value that can't be `.call`-ed, but this
  ;; will show a nicer error.
  (defn ^:private my-super-cool-fn
    []
    (throw (js/Error. "This function should never be called!")))

  (h/setup-mocks [something-stateful (let [counter (atom 0)]
                                       (fn []
                                         (swap! counter (partial + 2))
                                         @counter))

                  something-else-stateful (let [counter (atom 0)]
                                            (fn []
                                              (swap! counter #(- % 2))
                                              @counter))

                  my-super-cool-fn (constantly nil)])

  (it "works"
    (is (= 2 (something-stateful)))
    (is (= 4 (something-stateful)))

    (is (= -2 (something-else-stateful)))
    (is (= -4 (something-else-stateful))))

  (it "reinstantiates each mock for each test case in scope"
    (is (= 2 (something-stateful)))
    (is (= -2 (something-else-stateful)))))

(describe "async"
  (it "should support basic `await` usage"
    (let [cb (spy)
          timer (js/setInterval cb)]
      (h/async
       (is (m/called-times? cb 0))

       (await (next-macrotask+))

       (is (m/called-times? cb 1))

       (await (next-macrotask+))

       (is (m/called-times? cb 2))

       (js/clearInterval timer))))

  (it "should return a promise even with a non-promise value"
    (js/expect.assertions 1)

    (.. (h/async 7)
        (then (fn [resolved]
                (is (= 7 resolved))))))

  (it "should support arbitrary `await`-ed bindings inside of `let`"
    (h/async
     (let [value-1 {:a-key {:b-key "yeah dude"}}
           value-2 (await (js/Promise.resolve value-1))
           {value-3 :a-key} value-2
           {value-4 :b-key} (await (js/Promise.resolve value-3))]

       (is (= {:a-key {:b-key "yeah dude"}} value-1))
       (is (= {:a-key {:b-key "yeah dude"}} value-2))
       (is (= {:b-key "yeah dude"} value-3))
       (is (= "yeah dude" value-4))))))
