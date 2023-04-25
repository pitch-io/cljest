# Mocking

`cljest` comes with built-in support for making both spies and setting up mocks for your tests.

# Spying

`cljest` exposes a `cljest.core/spy` function, which is a wrapper on top of [`jest.fn()`](https://jestjs.io/docs/mock-function-api/#jestfnimplementation). By default (called with no arguments) it's an empty spy and is mainly useful for capturing inputs or acting as a function where its return result doesn't matter (like an event handler). You can optionally call it with an implementation, which is a function which will be called whenever the function is called.

## Example

```clj
(ns app.main-test
  (:require [cljest.core :refer [describe is it spy]]
            [cljest.matchers :as m]))

(describe "spying"
  (it "should capture calls"
    (let [my-spy (spy)]
      (my-spy "abc" 1 2 3)
      (is (m/called-with? my-spy "abc" 1 2 3))))

  (it "should accept an implementation"
    (let [my-spy (spy (constantly "the best result"))]
      (is (= "the best result" (my-spy)))
      (is (m/called-times? my-spy 1)))))
```

# Mocking

`cljest` supports `with-redefs` as well as exposes a more powerful set of mocking macros that work in a larger variety of tests.

So, `with-redefs` works as expected -- if your test or function is synchronous, `with-redefs` works as normal. However, if you've written an asynchornous test, you know that `with-redefs` won't work as expected and the mock will go away before the promise resolves, which isn't so useful if the test is async.

To help with this problem, `cljest` exposes two mocking helpers, `cljest.helpers.core/with-mocks` and `cljest.helpers.core/setup-mocks`.

`with-mocks` with identically to `with-redefs`, except that it supports async bodies, and will wait until the body resolves before removing the mock.

`setup-mocks` is kind of like `use-fixtures` but specific for mocks. You can conceptually consider it like a combination of `with-mocks` and `before-each`, letting you create a set of mocks for multiple tests.

## Example

```clj
(ns app.main-test
  (:require [cljest.core :refer [describe is it spy]]
            [cljest.helpers.core :as h]
            [cljest.matchers :as m]))

(defn ^:private wait+
  [ms]
  (js/Promise. (fn [res]
    (js/setTimeout res ms))))

(defn ^:private trigger-event
  [event-type value]
  ;; Assume effect/trigger exists
  (event/trigger event-type value))

(defn ^:private async-cb
  [cb effect-value]
  (trigger-event "before" effect-value)
  (h/async
    (await (wait+ 5))
    (trigger-event "after" effect-value)
    (cb)))

(describe "mocking"
  (describe "with-mocks"
    (it "can mock in async contexts"
      (h/with-mocks [trigger-event (spy)]
        (h/async
          (let [cb-promise (async-cb (constantly nil) "with-mocks")]
            (is (m/called-with? trigger-event "before" "with-mocks"))
            (await cb-promise)
            (is (m/called-times? trigger-event 2))
            (is (m/called-with? trigger-event "after" "with-mocks")))))))

  (describe "setup-mocks"
    (h/setup-mocks [trigger-event (spy)])

    (it "should work in this test"
      (h/async
        (await (async-cb (constantly nil) nil))
        (is (m/called-times? trigger-event 2))))

    (it "should work in this test too"
      (h/async
        (let [cb (spy)
              cb-promise (async-cb cb "setup-mocks")]
          (is (m/called-with? trigger-event "before" "setup-mocks"))
          (is (m/called-times? side-effect-fn 1))

          (await cb-promise)

          (is (m/called-with? trigger-event "after" "setup-mocks")))))))
```
