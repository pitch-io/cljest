(ns cljest.core
  (:require-macros [cljest.core])
  (:require ["@jest/globals" :refer [jest]]

            ;; If something is used by a macro that isn't directly imported by a file (such as formatters)
            ;; it should go in cljest.auxiliary
            cljest.auxiliary))

(defn spy
  ([] (.fn jest))
  ([mock] (.mockImplementation (.fn jest) mock)))

(defn spy-on
  "Creates a mock function similar to jest.fn but also tracks calls to (.-method-name object)
  Returns a Jest mock function."
  ([object method-name]
   (spy-on object method-name js/undefined))
  ([object method-name access-type]
   (.spyOn jest object method-name access-type)))

(defn is-matcher
  "The underlying matcher for `is`.

  Don't use this directly, use the `cljest.core/is` macro."
  [body-fn formatter]
  (.. (js/expect body-fn) (cljest__is formatter)))
