(ns cljest.helpers.reagent
  (:require ["@testing-library/react" :as rtl]
            [reagent.core :as r]))

(defn render
  "Takes a Reagent component and renders it.

  Example:

  ```
  (render [my-cool-component {:best-prop true}])
  ```"
  [component]

  (let [as-element #(r/as-element component)
        rendered (rtl/render (as-element))
        rerender (.-rerender rendered)]
    (r/after-render #(rerender (as-element)))))
