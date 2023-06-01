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

  (h/setup-mocks [something-stateful (let [counter (atom 0)]
                                       (fn []
                                         (swap! counter (partial + 2))
                                         @counter))

                  something-else-stateful (let [counter (atom 0)]
                                            (fn []
                                              (swap! counter #(- % 2))
                                              @counter))])

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

  (it "should support resolved bindings"
    (h/async
     (let [result (await (js/Promise.resolve "cool stuff!"))]
       (is (= result "cool stuff!")))))

  (it "should support multiple resolved bindings"
    (h/async
     (let [value-1 (await (js/Promise.resolve "cool stuff!"))
           value-2 "some-non-promise"
           value-3 (await (js/Promise.resolve "more cool stuff!"))]
       (is (= value-1 "cool stuff!"))
       (is (= value-2 "some-non-promise"))
       (is (= value-3 "more cool stuff!"))))))
