(ns cljest.core
  (:require [cljest.format :as formatter]
            [cljs.analyzer.api :as analyzer.api]
            cljs.env))

;; TODO(jo-sm): do we want to use a `jest-config.edn` or similar file?
(def user-defined-formatters-ns (some-> (get-in @cljs.env/*compiler* [:options :external-config :jest :formatters-ns])
                                        symbol))

(when user-defined-formatters-ns
  (require `[~user-defined-formatters-ns]))

(defmacro describe
  "Describes a block of tests. Any `before-each`/`after-each` inside of this block will be scoped to it.

  Example:

  ```
  (describe \"some-fn\"
    (before-each (do-something))
    (after-each (cleanup))

    (it \"does something\" ...))
  ```"
  [name & body]
  `(js/describe ~name (fn []
                        ~@body
                        js/undefined)))

(defmacro ^:private base-it
  "Macro that handles any `it` like functions from Jest. For example, `it` and `it.only`.

  See `it` and `only` below."
  [f description & body]
  (let [all-but-last-calls (drop-last body)
        last-call (last body)]
    `(~f ~description (fn []
                        ; This is usually enforced with eslint, but we can't use it in cljs-land, so instead we enforce it using expect
                        (js/expect.hasAssertions)

                        ~@all-but-last-calls

                        ; Jest only allows undefined and Promise instances to be returned from the body of `it`. However, we also
                        ; want to allow `nil`, so if the last call inside of the `it` call is `nil`, return `js/undefined` instead.
                        (let [last-call-result# ~last-call]
                          (if (nil? last-call-result#)
                            js/undefined
                            last-call-result#))))))

(defmacro is
  "A generic assertion macro for Jest. Asserts that `body` is truthy.

  Note: This does not work exactly like `clojure.test/is`. It does not accept `thrown?` or `thrown-with-msg?`.

  Example:

  (it \"should be true\"
    (is (= true (my-fn :some-keyword)))"
  [full-body]
  ;; TODO(jo-sm): support (is true)
  (let [negated? (= 'not (first full-body))
        body (if negated?
               (second full-body)
               full-body)
        matcher-sym (first body)
        resolved-symbol-name (get (analyzer.api/resolve &env matcher-sym) :name 'unknown-symbol)]
    ;; For the actual assertion, we want the full body, but for the formatter, we want to pass the possibly inner part
    ;; of (not (...)) to simplify writing the macro.
    `(cljest.core/is-matcher #(do ~full-body) ~(formatter/format resolved-symbol-name body negated?))))

(defmacro it
  "A single test case."
  [name & body]
  `(base-it js/test ~name ~@body))

(defmacro only
  "Like `it` but is the only test that runs in this file."
  [name & body]
  `(base-it js/test.only ~name ~@body))

(defmacro todo
  "Similar to a semi TODO, allows writing the name of a test case before writing the actual case."
  [name]
  `(js/it.todo ~name))

(defmacro skip
  "Like `it`, but skips the test. Can be used to temporarily skip flaky tests."
  [name & body]
  `(base-it js/test.skip ~name ~@body))

(defmacro before-each
  "Runs `body` before each test in the current scope."
  [& body]
  `(js/beforeEach (fn [] ~@body)))

(defmacro after-each
  "Runs `body` after each test in the current scope."
  [& body]
  `(js/afterEach (fn [] ~@body)))

(defmacro before-all
  "Runs `body` before all tests in the current file."
  [& body]
  `(js/beforeAll (fn [] ~@body)))

(defmacro after-all
  "Runs `body` after all tests in the current file."
  [& body]
  `(js/afterAll (fn [] ~@body)))

(defmacro each
  "A nicer way to create the same test case for multiple inputs. Essentially `doseq` for
  test cases, except the second argument is the description passed into the generated `it`
  block.

  Example:

  ```
  (each [[text num] [\"hello\" 1]
                    [\"world\" 2]
                    [\"foo\" 3]]
    (str \"should call the function with \" text \" and \" num)
    (do-something-with text num))
  ```

  Above generates:

  ```
  (it \"should call the function with hello and 1\" (do-something-with \"hello\" 1))
  (it \"should call the function with world and 2\" (do-something-with \"world\" 2))
  (it \"should call the function with foo and 3\" (do-something-with \"foo\" 3))
  ```"
  [seq-exprs name & body]
  `(do
     (doseq ~seq-exprs (it ~name ~@body))

     ; Return `js/undefined` since `doseq` returns nil and describe blocks expect nothing to be returned
     ; from them.
     js/undefined))

(defmacro only-each
  "A combination of `each` and `only`."
  [seq-exprs name & body]
  `(do
     (doseq ~seq-exprs (only ~name ~@body))
     js/undefined))
