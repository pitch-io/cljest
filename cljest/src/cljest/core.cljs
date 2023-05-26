(ns cljest.core
  (:require-macros [cljest.core])
  (:require ["@jest/globals" :refer [jest]]

            ;; If something is used by a macro that isn't directly imported by a file (such as formatters)
            ;; it should go in cljest.auxiliary
            cljest.auxiliary))

(def ^:dynamic *inside-is?* false)
(def ^:dynamic *is-body-negated?* false)

(defn spy
  "An unused spy, optionally taking `mock-implementation`, a function that will be called
  when this spy is called."
  ([] (.fn jest))
  ([mock-implementation] (.mockImplementation (.fn jest) mock-implementation)))

(defn spy-on
  "Creates a mock function similar to jest.fn but also tracks calls to (.-method-name object).

  Essentially it overrides a specific property on an object while preserving the original
  function.

  See https://jestjs.io/docs/jest-object#jestspyonobject-methodname for more details."
  ([object method-name]
   (spy-on object method-name js/undefined))
  ([object method-name access-type]
   (.spyOn jest object method-name access-type)))
