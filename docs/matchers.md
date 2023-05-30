# Matchers

[Matchers are the way Jest deals with assertions](https://jestjs.io/docs/using-matchers), and are the functions that are defined on `expect`:

```js
// `toBe` is the matcher here
expect(1).toBe(1);
```

`cljest` has some built-in matchers, primarily coming from [`jest-dom`](https://github.com/testing-library/jest-dom), and is extendable, allowing for you to define your own custom matchers in your code without needing to include them in `cljest` directly.

# How do I use matchers?

Matchers act just like another assertion: you wrap it in `is` and Jest will assert when it's called:

```clj
(require '[cljest.core :refer [it is]]
         '[cljest.helpers.dom :as h.dom]
         '[cljest.matchers :as m]
         '[uix.core :refer [$ defui]])

(defui my-cool-component
  []
  ($ :div.blue "hello world"))

(it "should have the `blue` class when initially rendered"
  (h.dom/render ($ my-cool-component))
  (is (m/has-class? (h.dom/get-by :text "hello world") "blue")))
```

If you don't wrap your matcher in `is`, you'll get an error; matchers must be wrapped in `is` to work.

## Negation

Matchers can also be negated using `not`:

```clj
(it "should not have the `red` class when initially rendered"
  (h.dom/render ($ my-cool-component))
  (is (not (m/has-class? (h.dom/get-by :text "hello world") "red"))))
```

# Built-in matchers

By default, `cljest` includes matchers from [`jest-dom`](https://github.com/testing-library/jest-dom), such as `toBeVisible`, `toHaveClass`, `toBeValid`, as well as a few assertions for `spy` calls like `called-with?`. These matchers live in [`cljest.matchers`](https://github.com/pitch-io/cljest/blob/master/cljest/src/cljest/matchers.cljs), and so for more details about which matchers are available, please look at the defined matchers in the `cljest.matchers` namespace.

# How do I make my own matcher?

In the event there's a matcher you'd like to use that's not included in `cljest`, you can use the macro `cljest.matcher/defmatcher` to define your matcher. This macro is like `def`, and takes the symbol (like `has-class?`) and the underlying matcher name (like `toHaveClass`):

```clj
(ns app.custom-matchers
  (:require-macros [cljest.matchers :refer [defmatcher]]))

(defmatcher has-class? "toHaveClass")
```

That's all you need to do! The rest is handled internally, including support negation, and basically this macro defines a function that has some metadata that's used during compilation to treat it as a matcher rather than a non-matcher assertion inside of `is`.
