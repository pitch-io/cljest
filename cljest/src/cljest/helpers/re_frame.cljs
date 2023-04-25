(ns cljest.helpers.re-frame
  (:require-macros [cljest.helpers.re-frame])
  (:require [cljest.helpers.core :refer [with-mocks]]
            [lambdaisland.deep-diff2 :as deep-diff2]
            [re-frame.core]
            [re-frame.interop]
            [re-frame.router]
            [re-frame.subs]
            [re-frame.trace]))

(defn ^:private generate-diff
  "Generate a pretty diff of the two given values."
  [a b]
  (-> (deep-diff2/diff a b)
      deep-diff2/pretty-print
      with-out-str))

;; `*processing-events?*` (in original `re-frame` core `*handling*`) is used in `re-frame`
;; to make sure that you don't `dispatch-sync` inside the events. We are removing this requirement
;; because all dispatches become `dispatch-sync` for our case
(def ^{:dynamic true, :private true} *processing-events?* false)

(def ^:private jest-queue (atom re-frame.interop/empty-queue))
(defn- dequeue!
  "Dequeue an item from a persistent queue which is stored as the value in
  `queue-atom`. Returns the item and updates the atom with the new queue
  value. If the queue is empty, returns nil.

  Code copied from [re-frame-test](https://github.com/day8/re-frame-test/blob/master/src/day8/re_frame/test.cljc#L16)"
  [queue-atom]
  (let [queue @queue-atom]
    (when (seq queue)
      (if (compare-and-set! queue-atom queue (pop queue))
        (peek queue)
        (recur queue-atom)))))

(defn synchronous-dispatch
  [argv]
  ;; Code copied from [`re-frame-test`](https://github.com/day8/re-frame-test/blob/master/src/day8/re_frame/test.cljc#L284)
  (swap! jest-queue conj argv)
  (when-not *processing-events?*
    (binding [*processing-events?* true]
      (loop []
        (when-let [queue-head (dequeue! jest-queue)]
          (re-frame.router/dispatch-sync queue-head)
          (recur))))))

(defn enable-subscription-debugging
  "Enables subscription debugging on the provided `cb` function. Logs the first attempt to deref
  the subscription as well as subsequent derefs that result in a different subscription value. If
  a deref does not result in a different value, it isn't logged.

  Prefer the `with-subscription-debug` macro over this function. You can use it as a wrapping macro
  (like `with-redefs`), which is nicer to work with."
  [cb]
  (let [!latest-subs-state (atom {})]
    (with-mocks [re-frame.trace/trace-enabled? true]
      (add-watch re-frame.trace/traces :subs-watch (fn [_ _ _ state]
                                                     (let [{op-type :op-type {query-v :query-v value :value} :tags} (last state)]
                                                       (when (= :sub/run op-type)
                                                         (let [sub-previously-called? (contains? @!latest-subs-state query-v)
                                                               prev-sub-state (get @!latest-subs-state query-v)]
                                                           (cond
                                                             (and sub-previously-called? (not= value prev-sub-state))
                                                             (prn query-v " : " (generate-diff value prev-sub-state))

                                                             (not sub-previously-called?)
                                                             (do
                                                               (swap! !latest-subs-state assoc query-v value)
                                                               (prn query-v " : " value))))))))

      (-> (js/Promise.resolve)
          (.then cb)))))
