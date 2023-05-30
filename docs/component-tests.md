# Component tests

Jest can do more than testing units; it has support for component tests, tests which simulate actions, such as clicking, typing, etc., on your components. While Jest does not use a browser, a very feature rich pure JavaScript DOM implementation called [JSDOM](https://github.com/jsdom/jsdom) can be very easily used by Jest.

# Setup

Jest does not enable JSDOM or DOM testing by default, and there are a few libraries that you need to write component tests in `cljest`, so you'll need to do a little more setup.

## JSDOM setup

First, you'll need to do Install `jest-environment-jsdom`:

```
npm install --save-dev jest-environment-jsdom
```

And then add this environment to your `jest.config.js` file in the `testEnvironment` key:

```js
// jest.config.js
module.exports = {
  // ... truncated ...
  testEnvironment: 'jest-environment-jsdom'
}
```

For more details, see the [`testEnvironment` docs from Jest](https://jestjs.io/docs/configuration#testenvironment-string).

## Additional support libraries

Okay, so now Jest will have a DOM, complete with `global.window`, `global.document`, etc, but to be able to render your components, you'll need a few more helper libraries.

Internally `cljest` uses `@testing-library/react`, `@testing-library/user-event`, and `@testing-library/jest-dom` for its DOM and React test helpers, so you'll need to install those:

```
npm install --save-dev @testing-library/react @testing-library/user-event @testing-library/jest-dom
```

And you'll need to require `@testing-library/jest-dom` in your setup file. If you didn't configure one, or didn't create a `cljest.edn` config file yet, see the [Configuration docs](./configuration.md) for more details.

Require `@testing-library/jest-dom` in your setup file like this:

```clj
(ns app.jest-setup
  (:require cljest.setup
            ["@testing-library/jest-dom"]))
```

# Getting started

Now you're ready to write a component test! So let's write one. In these examples we're using [UIx](https://github.com/pitch-io/uix) but you can switch for Reagent or Helix. If you're using Reagent, note you'd need to use `cljest.helpers.reagent/render` for the rendering method in the examples below.

Imagine you're building [Cookie Clicker](https://orteil.dashnet.org/cookieclicker/), and you're starting out by prototyping a component that, when you click a button, the counter shown increases.

```clj
(ns cookie-clicker.main
  (:require [uix.core :refer [$ defui] :as uix]))

(defui the-bakery
  []
  (let [[count set-count!] (uix/use-state 0)]
    ($ :div
      ($ :h1 (str count " cookies"))
      ($ :button {:on-click #(set-count! inc)} "Bake some cookies"))))
```

Okay, how do we test it? To test it, we need to render our component and simulate a click. Keep in mind that the latest version of `@testing-library/user-event` is async, so we also need to treat the promises correctly.

```clj
(ns cookie-clicker.main-test
  (:require [cljest.core :refer [describe it]]
            [cljest.helpers.core :as h]
            [cljest.helpers.dom :as h.dom]
            [cljest.matchers :as m]
            [cookie-clicker.main :as main]
            [uix.core :refer [$]]))

(describe "the-bakery"
 (it "should increment the count when the button is clicked"
  (h/async
    (h.dom/render ($ main/the-bakery))
    (is (m/visible? (h.dom/get-by :text "0 cookies")))
    (await (h.dom/click+ (h.dom/get-by :text "Bake some cookies")))
    (is (m/visible? (h.dom/get-by :text "1 cookies"))))))
```

Looking at the above, you'll notice a few things:

- We're rendering our component, `main/the-bakery`, very similarly to how we do in our normal code, and you don't need to keep a reference to it later. Instead, `h.dom/get-by` (and other query functions) use the ["screen"](https://testing-library.com/docs/queries/about#screen), the `document.body`, when looking for elements.
- We queried by text rather than a testing ID. Whenever possible, it's best to query for elements by their ARIA role, text, or something visible or spec defined, and query by the test ID as a last resort. For some details about when to use which queries, see the [`testing-library` query priority docs](https://testing-library.com/docs/queries/about#priority).
- We're testing the component from the end user's perspective. We don't know that internally it's using `use-state`, and for all we know, it could be using `re-frame` or another state management library.
- We're using a matcher to assert that the element is visible, which is coming from `cljest.matchers`. For more details about matchers, please see the [`cljest.matchers` namespace](https://github.com/pitch-io/cljest/blob/master/cljest/src/cljest/matchers.cljs) and the [matchers docs](./matchers.md).

## Going further - more cookies!!

But, as we know from the original game, the fun aspect of Cookie Clicker isn't that you just click the button for more cookies, it's that you can get more cookies by spending your existing cookies, allowing you to grow and get N cookies per second automatically. We won't simulate the whole experience, but what if we wanted to introduce a rudimentary version of this?

Let's say that when you click the button, we give you another cookie in one second.

First, we need to modify our component a little:

```clj
(ns cookie-clicker.main
  (:require [uix.core :refer [$ defui] :as uix]))

(defui the-bakery
  []
  (let [[count set-count!] (uix/use-state 0)
        timeout-ids (uix/use-ref [])
        handle-click (fn []
                       (set-count! inc)

                       (let [timeout-id (js/setTimeout #(set-count! inc) 1000)]
                         (swap! timeout-ids conj timeout-id)))]

    (uix/use-effect
     (fn []
       (fn []
         (doseq [timeout-id @timeout-ids]
           (js/clearTimeout timeout-id))))
     [])

    ($ :div
       ($ :h1 (str count " cookies"))
       ($ :button {:on-click handle-click} "Bake some cookies"))))
```

Now, how can we test this, and more importantly, how can we avoid needing to wait an actual second or two to assert our expectations?

Thankfully Jest has the ability to mock timers, and so does `cljest`, via the `cljest.timers` namespace. Let's use it and write our new test. We'll also do a little refactoring since we're reusing some of the same code multiple times now.

```clj
(ns cookie-clicker.main-test
  (:require [cljest.core :refer [before-each describe it]]
            [cljest.helpers.core :as h]
            [cljest.helpers.dom :as h.dom]
            [cljest.matchers :as m]
            [cljest.timers :as t]
            [cookie-clicker.main :as main]
            [uix.core :refer [$]]))

(describe "the-bakery"
  (before-each (h.dom/render ($ the-bakery)))

  (it "should increment the count when the button is clicked"
    (h/async
     (is (m/visible? (h.dom/get-by :text "0 cookies")))
     (await (h.dom/click+ (h.dom/get-by :text "Bake some cookies")))
     (is (m/visible? (h.dom/get-by :text "1 cookies")))))

  (it "should add a new cookie every second after the button is clicked"
    (t/with-fake-timers
      (h/async
       (await (h.dom/click+ (h.dom/get-by :text "Bake some cookies")))
       (is (m/visible? (h.dom/get-by :text "1 cookies")))

       (t/advance-timers-by-time 1000)
       (await (h.dom/wait-for+ #(is (m/visible? (h.dom/get-by :text "2 cookies")))))))))
```

Not only can we test our component is doing what we expected, but we could test our code without needing to wait 1 second or longer to validate the test due to our fake timers:

```
  the-bakery
    ✓ should increment the count when the button is clicked (43 ms)
    ✓ should add a new cookie every second after the button is clicked (16 ms)
```

That second saved can now be used to write more tests. Just kidding.

# Further thoughts on component tests

Now that you have your feet wet about writing some component tests in Jest, you might have some thoughts or questions that have come to mind, especially if you're coming from a place that's used more (or exlusively) E2E tests, or coming from a place where you're used to testing the business or state logic (such as re-frame) instead of the view. With that in mind, below are some thoughts about component tests and how they might fit into your testing setup.

## Why write component tests? If my views are pure, shouldn't testing my state changes, events, etc. be sufficient?

In theory, yes, but in practice, it's not so simple and is too myopic.

So: yes, if your views are pure, having no internal state and are purely a reflection of some input, you can validate that given some state X the output of the component is Y.

However, besides the fact that this is often not actually reality -- how often do you use `ratom` or `use-state`? -- this isn't what's actually valuable to you. Validating that your component renders the right thing is only part of the task, and the other part is validating the business logic from the perspective of the end user, that when that button is clicked, an event is fired which causes a modal or an API request to happen. Basically, testing from the perspective of the user using the component validates not only that your component is rendering the right stuff, but that you've wired up the right events in the right places.

Thinking about and testing components in this way gives you confidence that your component is working, and also makes your code more resilient to future changes that don't affect the way the component behaves. For example, if you switch state management libraries, as long as your button still performs that API call when clicked, it doesn't matter that you've switched from X to Y, your test will still pass, basically helping you forget a bit about those implementation details.

Basically: [The more your tests resemble the way your software is used, the more confidence they can give you.](https://testing-library.com/docs/guiding-principles)

## Okay, but why not simply test my application in a browser in an end-to-end test?

End-to-end (E2E) tests have their place, and are very valuable for testing the "happy path" of a complex state in your application. They also do things that other "levels" of your testing architecture can't since E2E tests by definition rely on running backend and service application code.

But E2E tests don't scale very well. Running a browser is expensive, and so if you start to have a large number of tests, running them all becomes costly, and if you're running them in the CI, might require you to figure out how to split your tests into multiple runners to get around the tests taking a long time to run (or rely on an expensive third party to handle this for you). Your tests also require the entire running application, even for something as simple as clicking a button, and if you wanted to make whatever your button does fail to see the non-happy path, good luck. We don't even need to get started on mutable state.

In contrast, component tests scale more easily than E2E tests since they don't run in the browser, and so you can more easily do non-happy path tests for your component. You're able to mock certain aspects of your system that aren't so relevant for a specific component, and you get faster feedback, which is especially important during development; a single E2E test might take an order of magnitude longer than a component test.

Long story short: E2E tests have their place, and you don't need to (and shouldn't!) replace all of your E2E tests with component tests, but you can test more complicated scenarios for a single component or a handful of components more easily and cheaply with a component test than with an E2E test, and you can also get feedback faster.

# Limitations

As mentioned above, Jest does not use a browser, and JSDOM is a pure JavaScript implementation of the DOM, so it has some inherit limitations. Specifically:

- Anything that deals with layouts isn't supported. JSDOM doesn't actually calculate the layout of a DOM node and thus doesn't have information like the bounding box.
- Navigation in general.

You can see [more details on the official JSDOM repository](https://github.com/jsdom/jsdom#unimplemented-parts-of-the-web-platform).

Basically, you can't assume that everything that works in the browser will work in Jest with JSDOM.

However, even for complex applications and components, this is rarely an issue, and even less so if you write your components in a testable way. Even at Pitch, with a very user-interaction heavy application, we've found these limitations to not be particularly limiting in practice and the complexity comes from other aspects of our code.

# Appendix

## DOM vs. React naming

Above, you might have noticed that `cljest` exposes component helpers from the `cljest.helpers.dom` namespace, even though we're technically dealing with React components. At the same time, we do make the distinction between Reagent and React by the use of `cljest.helpers.reagent/render` rather than `cljest.helpers.dom/render` for Reagent components. `cljest.helpers.dom` also includes things that aren't really DOM related, like "user generated" events. What gives?

This question isn't without merit: in some sense, `cljest.helpers.dom` is a bit heavy and includes a lot of things that could otherwise exist in separate namespaces, and it does conflate DOM with React.

After some thought, we came to the conclusion that the tradeoff between correctness and ease of use was justified here. When dealing with components in ClojureScript, you're almost certainly going to be using React, and while other non-React or lower level DOM libraries do exist, they're relatively rare in usage. To simplfy things, and to not make the namespace too React specific, we opted to use `dom` as the name, rather than `react`. We thought about `cljest.helpers.components` but that's a lot to type when `dom` suffices.

We also concluded that it made sense to colocate the event helpers such as `click` here as well, rather than keeping a separate namespace, since when dealing with components, you're almost always going to be doing something interesting to them (clicking, typing, etc.), and it's one less namespace to require.

Lastly, the separate `cljest.helpers.dom/render` and `cljest.helpers.reagent/render` reasoning is simple: if you're using UIx, you shouldn't need to add Reagent as a dependency just to use `cljest.helpers.dom` (which would be necessary due to the additional code needed to get Reagent to work correctly with React Testing Library).

## `@testing-library/user-event` versioning and promises

[Recently `@testing-library/user-event` made a major change in between v13 and v14, making all functions asynchronous](https://github.com/testing-library/user-event/issues/504). To avoid any potential issues with using the older v13 library and then later upgrating to v14, `cljest` makes all exposed `user-event` helpers asynchronous, both in name (appending `+` to the function) and by ensuring the result is always a promise.

