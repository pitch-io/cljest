(ns cljest.helpers.core
  (:require [cljest.core :refer [after-each before-each]]))

(defmacro ^:private with-scoped-redefs
  "Similar to `with-redefs` but allows arbitrarily beginning and ending when the bindings are mocked and reset by calling
  `start` and `finish` respectively."
  [start finish bindings & body]
  (let [names (take-nth 2 bindings)
        vals (take-nth 2 (drop 1 bindings))
        wrapped-vals (map (fn [v] (list 'fn [] v)) vals)
        orig-val-syms (for [_ names] (gensym))
        temp-val-syms (for [_ names] (gensym))
        binds (map vector names temp-val-syms)
        redefs (reverse (map vector names orig-val-syms))
        bind-value (fn [[k v]] (list 'set! k (list v)))]
    `(let [~@(interleave orig-val-syms names)
           ~@(interleave temp-val-syms wrapped-vals)
           ~start #(do ~@(map bind-value binds))
           ~finish #(do ~@(map bind-value redefs))]
       ~@body)))

(defmacro setup-mocks
  "Similar to `with-redefs`, but allows for mocking at the top level or within a describe block without needing to wrap
  individual tests in `with-redefs`/`with-redefs+`. Set up and automatically cleans up mocks before and after each test.

  Note: this function does not work inside of an `it` block. If you need to mock something for an individual test case,
  use `with-mocks`.

  Example:
  ```clj
  (describe \"My test\"
    (h/setup-mocks [api-client/http-post+ (spy #(js/Promise.resolve))])

    (it \"should make a request when called\"
      (some-requesting-fn)

      (m/called-with? api-client/http-post+ body)))
  ```"
  [bindings]
  `(with-scoped-redefs start# finish# ~bindings
     (before-each (start#))
     (after-each (finish#))))

(defmacro with-mocks
  "Similar to `with-redefs` but handles bodies that may have promises."
  [bindings & body]
  `(with-scoped-redefs start# finish# ~bindings
     (-> (js/Promise.resolve)
         (.then start#)
         (.then #(do ~@body))
         (.finally finish#))))

(defn ^:private group
  "Creates a new 'group' map that has `forms` and `bindings` keys."
  ([] (group [] []))
  ([forms] (group forms []))
  ([forms bindings] {:forms forms :bindings bindings}))

(defn ^:private await-form?
  "Returns true if the given `form` is a sequence and the first element is `'await`."
  [form]
  (and (seq? form) (= 'await (first form))))

(defn ^:private let-form?
  "Returns true if the given `form` is a sequence and the first element is `'let`."
  [form]
  (and (seq? form) (= 'let (first form))))

(defn ^:private unwrap-await
  "If `form` is an `await`-wrapped form, return what it is wrapping."
  [form]
  (if (await-form? form)
    (second form)
    form))

(defn ^:private forms->groups
  "Takes all `forms` inside of an `async` block and turns them into `groups`, which later get turned into `.then`
  calls."
  [forms]
  (let [{:keys [current prev]}
        (reduce
         (fn [{prev :prev {:keys [forms bindings]} :current} form]
           (cond
             ;; If the form itself is `(await ...)`, take the inner part and add it to the current group,
             ;; then add the current group to the `prev` sequence.
             (await-form? form)
             {:current (group)
              :prev (conj prev
                          (group (conj forms (second form)) bindings))}

             ;; If the form is `let` and any of the binding values has `(await ...)`, take the first binding pair
             ;; and use its value as the return value of the current group. Add a new group with the symbol as the
             ;; first binding.
             (and (let-form? form) (some await-form? (second form)))
             (let [all-bindings (second form)
                   first-binding-sym (first all-bindings)
                   first-binding-val (unwrap-await (second all-bindings))
                   rest-bindings (nthrest all-bindings 2)
                   let-forms (nthrest form 2)

                   ;; If there aren't any more bindings (which would generate `(let [] forms)`),
                   ;; use `forms` instead of creating another `let`.
                   next-async-expr (if (empty? rest-bindings)
                                     (concat ['cljest.helpers.core/async] let-forms)
                                     (list 'cljest.helpers.core/async (concat (list 'let rest-bindings) let-forms)))]
               {:current (group)
                :prev (conj prev
                            (group (conj forms first-binding-val))
                            (group [next-async-expr] [first-binding-sym]))})

             ;; If the form is `let` but there aren't any `await` calls in the binding values, just create a new
             ;; `async` wrapped group.
             (let-form? form)
             {:current (group)
              :prev (conj prev
                          (group
                           (conj forms (list 'let (second form) (concat ['cljest.helpers.core/async] (nthrest form 2))))
                           bindings))}

             ;; Otherwise, add the current form to the current group's forms.
             :else
             {:current (group (conj forms form) bindings)
              :prev prev}))
         {:current (group)
          :prev []}
         forms)]

    ;; Prevent unnecessary functions from being added to the result
    (if (empty? (:forms current))
      prev
      (conj prev current))))

(defmacro async
  "Similar to JS's async/await. Wraps the body of `async` in a promise and allows for the use
  of `await`, which when called will wait for the promise to finish before continuing execution
  of the promise body.

  Allows `await` in a few cases:

  *Top level*:

  ```
  (async
    (await my-promise)
    (some-fn))
  ```

  *Inside of `let`*:

  ```
  (async
    (let [a-keyword :kw]
      (await (an-async-fn a-keyword))
      (some-fn)))
  ```

  *Inside of the binding value of `let`*:

  ```
  (async
    (let [a-keyword :kw
          my-async-binding (await my-promise)]
      (some-fn)))
  ```
  "
  [& forms]
  (let [groups (forms->groups forms)
        ;; We can assume there are no bindings for the first group, since bindings necessarily must come
        ;; from a previous group.
        first-forms (:forms (first groups))
        then-groups (->> groups
                         rest
                         (map (fn [{:keys [forms bindings]}]
                                (list '.then (concat (list 'fn (apply vector bindings)) forms)))))]

    `(do ~@(butlast first-forms)
         (let [beginning# ~(last first-forms)]
           ;; This avoids creating a new Promise instance if `beginning#` is thennable
           (-> (if (and beginning# (.-then beginning#))
                 beginning#
                 (js/Promise.resolve beginning#))
               ~@then-groups)))))
