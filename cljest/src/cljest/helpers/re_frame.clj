(ns cljest.helpers.re-frame)

(defmacro with-subscription-debug
  "Enables subscription debugging on `body`. See `enable-subscription-debugging` for
  more details about how the debugging works.

  Example:
  ```clj
  (it \"should render correctly\"
    (h/with-subscription-debug
      (h/render [my-stateful-component])
      ...))

  ```"
  [& body]
  `(cljest.helpers.re-frame/enable-subscription-debugging #(do ~@body)))
