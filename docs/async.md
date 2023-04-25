# Async code

Jest has full, uncompromised support for promises. Returning a promise inside of `it` will result in the test being treated as asynchronous, and you can have multiple async tests within a single `describe` block, so besides the usual things to keep in mind when working with promises, there's not much you need to keep in mind that's specific for Jest and `cljest`.

## Example

```clj
(ns app.utils-test
  (:require [cljest.core :refer [describe is it spy]]
            [cljest.matchers :as m]
            [app.utils :as utils]))

(defn ^:private wait-for+
  "Waits for `cb` to not throw"
  [cb]
  (js/Promise. (fn [res]
    (try
      (res (cb))
      (catch :default _
        (res (js/Promise. #(js/setTimeout %1 16))))))))

(describe "utils/fire-async"
  (it "calls cb async"
    (let [cb (spy)]
      (utils/fire-async cb)
      (is (m/not-called? cb))

      (wait-for+ #(is (m/called? cb))))))
```

Since `wait-for+` was called at the end of the test, and it returns a promise, this test will be treated as async and Jest will wait for it (for up to 5s, or the value you configure for [`testTimeout`](https://jestjs.io/docs/configuration#testtimeout-number)). No need to have a reference to something like `done`.

# `async` macro

However, most async tests are a bit more complicated than the above example and require chaining promises:

```clj
(-> (js/Promise.)
    (.then (fn []
      (some-async-fn)))
    (.then (fn [result]
      (a-different-fn)
      (some-other-async-fn result)))
    (.then (fn [second-result]
      (yet-another second-result))))
```

Although very contrived, it illustrates that chaining promises can be a bit cumbersome and can make it somewhat harder to read the code and understand the intent of what's going on.

To help make your code easier to read and work with, `cljest` includes a helpful `async` macro that works similarly to `async`/`await` in JavaScript:

```clj
(require '[cljest.helpers.core :as h])

(h/async
  (let [result (await (some-async-fn))]
    (a-different-fn)

    (let [second-result (await some-other-async-fn result)]
      (yet-another second-result))))
```

For more details, please see [the docstring for `cljest.helpers.core/async`](https://github.com/pitch-io/cljest/blob/5d19b87021023daef75971ff005e05a288369c1d/cljest/src/cljest/helpers/core.clj#L64-L95).

# When to choose `async` instead of something like Promesa?

There are libraries, such as [Promesa](https://github.com/funcool/promesa), that help make dealing with promises and async code in ClojureScript easier, and in most cases you can use such a library without issue.

However, it is easy to get caught in a situation where you need more more explicit control than these libraries give you, like you get with promise chaining. For example, imagine you have a test in which you assert that a button changes its text depending on the status of an API request. In this example, we're using UIx and `cljest.helpers.dom`, whose requires are ommitted for brevity:

```clj
(defui ^:private my-component
  []
  (let [[status set-status!] (uix/use-state nil)
        make-api-request+ (uix/use-callback #(api-client.post+ "/example") [])]
    ($ :<>
      ($ :button {:on-click (fn []
                              (set-status! :pending)
                              (-> (make-api-request+)
                                  (.then #(set-status! :success))
                                  (.catch #(set-status! :failure))))
                  :disabled (not (nil? status))}
        (case status
          nil "Make request"
          :pending "Loading..."
          :success "Success!"
          :failure "Failed :(")))))

(it "should show a loading indicator and change to success when complete"
  (h.dom/render ($ my-component))

  (-> (h.dom/click+ (h.dom/get-by :text "Make request"))
      (.then (fn []
               (is (m/disabled? (h.dom/get-by :text "Loading...")))))
      (.then (fn []
              (is (m/visible? (h.dom/get-by :text "Success!")))))))
```

In words, we assert that the button becomes disabled and changes text as soon as we click the button, and then on the next tick (when the promise resolves) the button changes to having "Success!" text.

This test is not so simple to write in Promesa. Promesa wraps each form inside of something like `do!` with a promise, meaning that by the time you'd attempt to assert that the button changed to "Loading..." it's already become "Success!" as the next tick has happened:

```clj
(require '[promesa.core :as p])

(it "should show a loading indicator and change to success when complete"
  (h.dom/render ($ my-component))

  (p/do!
    (h.dom/click+ (h.dom/get-by :text "Make request"))

    ;; The get-by call will fail as "Loading..." will have already changed to "Success!"
    ;; since Promesa wrapped the previous call in a promise implicitly.
    (is (m/disabled? (h.dom/get-by :text "Loading...")))
    (is (m/visible? (h.dom/get-by :text "Success!")))))
```

In other words, we need the explicit control of promise chaining. This is where the `async` macro can help: it reduces the verbosity of explicit promise chaining while still allowing you to have explicit control. In our case, we can also utilize `cljest.helpers.dom/wait-for+` to wait for the final assertion to pass:

```clj
(require '[cljest.helpers.core :as h])

(it "should show a loading indicator and change to success when complete"
  (h.dom/render ($ my-component))

  (h/async
    (await (h.dom/click+ (h.dom/get-by :text "Make request")))
    (is (m/disabled? (h.dom/get-by :text "Loading...")))
    (await (h.dom/wait-for+ #(h.dom/get-by :text "Success!")))
    (is (m/visible? (h.dom/get-by :text "Success!")))))
```

In this case we don't really need `async` either, as the `visible?` check is redundant since we're already effectively asserting its presence with `h.dom/get-by` and returning a promise (`wait-for+`) will make Jest treat the whole test as asynchornous. However, it's easy to imagine that the component is more complicated and we needed to perform assertions after the button changed and the API request passed, and this is where something like `async` becomes powerful.
