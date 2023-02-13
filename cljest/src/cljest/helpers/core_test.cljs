(ns cljest.helpers.core-test
  (:require [cljest.core :refer [describe is it]]
            [cljest.helpers.core :as h]
            [cyrik.cljs-macroexpand :refer [cljs-macroexpand-all] :rename {cljs-macroexpand-all macroexpand-all}]))

(describe "async"
  (it "should macroexpand into a resolves promise when called with nothing"
    (is (= (macroexpand-all '(js/Promise.resolve))
           (macroexpand-all '(h/async)))))

  (it "should add the provided form to the body of the `then` function"
    (is (= (macroexpand-all '(-> (js/Promise.resolve)
                                 (.then (fn []
                                          (fn-1)
                                          (fn-2 with-an-arg)))))
           (macroexpand-all '(h/async
                              (fn-1)
                              (fn-2 with-an-arg))))))

  (it "should handle the `await` keyword by separating the bodies with then"
    (is (= (macroexpand-all '(-> (js/Promise.resolve)
                                 (.then (fn []
                                          (fn-1)
                                          (async-fn-2)))
                                 (.then (fn []
                                          (fn-3 with-an-arg)))))

           (macroexpand-all '(h/async
                              (fn-1)
                              (await (async-fn-2))
                              (fn-3 with-an-arg))))))

  (it "should handle `await` inside of `let` and turn the let body into another `async` call"
    (is (= (macroexpand-all '(-> (js/Promise.resolve)
                                 (.then (fn []
                                          (async-fn-1)))
                                 (.then (fn []
                                          (let [name-1 :kw]
                                            (-> (js/Promise.resolve)
                                                (.then (fn []
                                                         (fn-2)
                                                         (async-fn-3 name-1)))))))
                                 (.then (fn []
                                          (fn-4)))))

           (macroexpand-all '(h/async (await (async-fn-1))
                                      (let [name-1 :kw]
                                        (fn-2)
                                        (await (async-fn-3 name-1)))
                                      (fn-4))))))

  (it "should turn await inside of a let binding value into Promise.all"
    (is (= (macroexpand-all '(-> (js/Promise.resolve)
                                 (.then (fn []
                                          (fn-1)
                                          (js/Promise.all [promise-1 non-promise promise-2])))
                                 (.then (fn [name-1 name-2 name-3]
                                          (-> (js/Promise.resolve)
                                              (.then (fn []
                                                       (fn-2)
                                                       (async-fn-3 name-3))))))
                                 (.then (fn []
                                          (fn-4)))))

           (macroexpand-all '(h/async (fn-1)
                                      (let [name-1 (await promise-1)
                                            name-2 non-promise
                                            name-3 (await promise-2)]
                                        (fn-2)
                                        (await (async-fn-3 name-3)))
                                      (fn-4))))))
  (it "should handle nested lets"
    (is (= (macroexpand-all '(-> (js/Promise.resolve)
                                 (.then (fn []
                                          (async-fn-1)))
                                 (.then (fn []
                                          (fn-2)
                                          (let [name-1 :kw]
                                            (-> (js/Promise.resolve)
                                                (.then (fn []
                                                         (fn-3 name-1)
                                                         (js/Promise.all [:kw-1 promise-1])))
                                                (.then (fn [name-1-1 name-2-2]
                                                         (-> (js/Promise.resolve)
                                                             (.then (fn []
                                                                      (fn-4 name-2-2))))))))))

                                 (.then (fn []
                                          (fn-5)))))

           (macroexpand-all '(h/async (await (async-fn-1))
                                      (fn-2)
                                      (let [name-1 :kw]
                                        (fn-3 name-1)
                                        (let [name-1-1 :kw-1
                                              name-2-2 (await promise-1)]
                                          (fn-4 name-2-2)))
                                      (fn-5)))))))

