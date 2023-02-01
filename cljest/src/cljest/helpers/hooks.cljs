(ns cljest.helpers.hooks
  (:require ["@testing-library/react-hooks" :as rhtl]))

(defn render-hook
  "Renders given `hook-fn`.

  Returns a map with keys `:result` and `:act`.
  * `:result` is an object that can be dereferenced which contains the
  current value of the rendered hook.
    You can consider `result` to be similar to an atom -- it is mutable and
    you need to dereference it to get the current value if it changes over the
    lifecycle of your test.
  * `:act` is a function that should wrap any mutations to the rendered hook.
   It essentially performs the action in such a way that any updates that need to
   be rendered are also performed. It takes a single function that contains mutations
   and the result of this function must be `js/undefined` or a function.
   For more details, see [the official docs on `act`](https://reactjs.org/docs/test-utils.html#act).

  Example:

  ```
  (let [{:keys [result act]} (render-hook (fn [] (uix/use-state 1)))
        [value set-value!] @result]
    (m/=? 1 value)

    (act (fn [] (set-value! 2)))

    (let [[value _set-value!] @result]
      (m/=? 2 value)))
  ```"
  [hook-fn]
  (let [render-result (rhtl/renderHook hook-fn)
        result (.-result render-result)
        waitFor (.-waitFor render-result)
        waitForValueToChange (.-waitForValueToChange render-result)
        waitForNextUpdate (.-waitForNextUpdate render-result)
        derefable-result (specify! result
                                   IDeref
                                   (-deref [this]
                                           (.-current ^js this)))]
    {:result derefable-result
     :act rhtl/act
     :wait-for+ waitFor
     :wait-for-value-to-change+ waitForValueToChange
     :wait-for-next-update+ waitForNextUpdate}))
