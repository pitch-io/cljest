# Differences and migration from `cljs.test` to `cljest`

`cljs.test` and `cljest` work somewhat similarly and can often be easily migrated. The following are likely most of the changes you need to do:

1. Use `cljest.core` (and related namespaces) and remove `cljs.test` namespaces. `cljs.test` is not supported in Jest.
1. Rename `deftest` to `describe`  and `testing` to `it`.
1. Use a string instead of a symbol for the first argument of the `describe` macro.
1. Move `it` to a top level if it's inside of a loop. `it` macros are not allowed to be looped.
1. Migrates `use-fixtures` to `before-each`, `after-each`, `before-all`, and `after-all`. `(use-fixtures :once ...)` is analagous to `before-all` and `after-all`, and `:each` analagous to `before-each` and `after-each`.
1. `with-redefs` is migrated to `cljest.helpers.core/with-mocks` and `setup-mocks`, depending on the use case.
1. Remove any promise wrappers for timeouts. Jest handles this automatically.

# Example

Take a test that starts a stateful service that increments a counter and performs tests against this service. In this example, we're using [`spy` from `alexanderjamesking`](https://github.com/alexanderjamesking/spy).

```clj
(ns app.service-test
  (:require [cljs.test :refer [deftest is testing use-fixtures]]
            [spy.core]
            [spy.assert]
            [app.service :as service]
            [app.other-service :as other-service]))

(def ^:private service-key :testing)

(use-fixtures :each {:before #(service/start service-key)
                     :after #(service/stop service-key)})

(deftest service-test
  (testing "the initial state is 0"
    (is (zero? (service/current-count service-key))))

  (testing "incrementing the count"
    (service/increment service-key)
    (is (= 1 (service/current-count service-key))))

  (testing "decrementing the count"
    (service/decrement service-key)
    (is (zero? (service/current-count service-key))))

  (testing "the service calls other-service/notify when incrementing"
    (with-redefs [other-service/notify (spy.core/spy)]
      (service/increment service-key)

      (spy.assert/called-with? other-service/notify service-key :count)))

  (testing "throws when encountering a service that hasn't started"
    (is (thrown? (service/current-count :something-else)))))
```

Migrating to `cljest` is fairly straightforward. As mentioned above, you need to mostly rename imports and migrate the fixtures:

```clj
(ns app.service-test
  (:require [cljest.core :refer [describe it is before-each after-each spy]]
            [cljest.matchers :as m]
            [app.service :as service]
            [app.other-service :as other-service]))

(def ^:private service-key :testing)

(describe "service"
  (before-each (service/start service-key))
  (after-each (service/stop service-key))

  (it "should have an initial state of 0"
    (is (zero? (service/current-count service-key))))

  (it "should increment the count when calling increment"
    (service/increment service-key)
    (is (= 1 (service/current-count service-key))))

  (it "should decrement the count when calling decrement"
    (service/decrement service-key)
    (is (zero? (service/current-count service-key))))

  (it "should call other-service/notify when incrementing"
    (with-redefs [other-service/notify (spy.core/spy)]
      (service/increment service-key)

      (is (m/called-with? other-service/notify service-key :count))))

  (it "should throw when encountering a service that hasn't started yet"
    (let [ex (atom nil)]
      (try
        (service/current-count service-key)
        (catch :default e (reset! ex e)))

      (is @ex))))
```

A few things to notice beyond the renaming:

- `before-each` and `after-each` are scoped to a describe block (or the top level). In this case it does not matter as there's only one set of test cases, but for test suites that have multiple sets of test cases it can be beneficial to keep your `before-` and `after-each` functions inside of the describe block(s) that need them.
- The `is` macro for `cljest` is very similar to but simpler than `cljs.test`'s version. It only performs truthy assertions and does not include `thrown?` as a possible allowed call.
- `spy` is built into `cljest.core`, which maps to [Jest's `jest.fn`](https://jestjs.io/docs/jest-object#jestfnimplementation) built in mocking mechanism.

# Async example

Expanding on the above example, let's say we expanded the incrementing functionality to make a call to yet another service, but does it asynchronously, and we want to test it. Let's also assume that there exists some kind of `with-redefs+` that would work with promises.

```clj
(ns app.service-test
  (:require [cljs.test :refer [deftest is testing use-fixtures async]]
            [spy.core]
            [spy.assert]
            [app.service :as service]
            [app.yet-another-service :as yet-another-service]))

(def ^:private service-key :testing)

(use-fixtures :each {:before #(service/start service-key)
                     :after #(service/stop service-key)})

(defn ^:private next-tick+
  []
  (js/Promise. (fn [resolve] (js/process.nextTick resolve))))

(deftest service-async-test
  (async done
    (with-redefs+ [yet-another-service/notify (spy.core/spy)]
      (service/increment service-key)
      (-> (next-tick+)
          (.then (fn []
            (spy.assert/called-with? yet-another-service/notify service-key)

            (done)))))))
```

Rewriting to Jest is, as above, straightforward and also simplifies the test. `(async done ...)` is not necessary as Jest automatically handles promises that are the return value of the test, and the test can be colocated with the other tests.

```clj
(ns app.service-test
  (:require [cljest.core :refer [describe it is before-each after-each spy]]
            [cljest.helpers.core :as h]
            [cljest.matchers :as m]
            [app.service :as service]
            [app.other-service :as other-service]
            [app.yet-another-service :as yet-another-service]))

(def ^:private service-key :testing)

(defn ^:private next-tick+
  []
  (js/Promise. (fn [resolve] (js/process.nextTick resolve))))

(describe "service"
  (before-each (service/start service-key))
  (after-each (service/stop service-key))

  ;; Other tests are hidden for brevity
  (it "should call yet-another-service asynchornously"
    (h/with-mocks [yet-another-service/notify (spy)]
      (service/increment service-key)

      (is (m/not-called? yet-another-service/notify))

      (-> (next-tick+)
          (.then (fn []
            (is (m/called-with? yet-another-service/notify service-key))))))))
```

This test could be simplified even further by using the `cljest.helpers.core/async` helper:

```clj
(it "should call yet-another-service asynchornously"
  (h/with-mocks [yet-another-service/notify (spy)]
    (h/async
      (service/increment service-key)
      (is (m/not-called? yet-another-service/notify))

      (await (next-tick+))

      (is (m/called-with? yet-another-service/notify service-key)))))
```
