(ns cljest.helpers.core
  (:require [cljest.core :refer [after-each before-each]]))

(defmacro ^:private with-scoped-redefs
  "Similar to `with-redefs` but allows arbitrarily beginning and ending when the bindings are mocked and reset by calling
  `start` and `finish` respectively."
  [start finish bindings & body]
  (let [names (take-nth 2 bindings)
        vals (take-nth 2 (drop 1 bindings))
        orig-val-syms (for [_ names] (gensym))
        temp-val-syms (for [_ names] (gensym))
        binds (map vector names temp-val-syms)
        redefs (reverse (map vector names orig-val-syms))
        bind-value (fn [[k v]] (list 'set! k v))]
    `(let [~@(interleave orig-val-syms names)
           ~@(interleave temp-val-syms vals)
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
  "Creates a new \"group\" map that has `forms` and `bindings` keys."
  ([] (group [] []))
  ([forms] (group forms []))
  ([forms bindings] {:forms forms :bindings bindings}))

(defn ^:private await-seq?
  "If the given `form`, as a list of quoted symbols, is eqv to `(list 'await ...)`"
  [form]
  (and (seq? form) (= 'await (first form))))

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
  [& body]
  (let [then-groups (->> body
                         (reduce
                          (fn [{rest :rest {:keys [forms bindings]} :current} form]
                            (cond
                              ;; If the form itself is `(await ...)`, take the `...`, add it to the current group,
                              ;; and add the current group to the `rest`.
                              (await-seq? form)
                              {:current (group)
                               :rest (conj rest (group (conj forms (second form)) bindings))}

                              ;; If the form is `let` and any of the binding values has `(await ...)`, add a new
                              ;; `js/Promise.all` to the rest and add the body of the `let` as a second new group,
                              ;; wrapped in `async`, with the binding names from the `let` as the arguments of the
                              ;; `.then` function.
                              (and (= 'let (first form)) (some await-seq? (second form)))
                              (let [bindings (second form)
                                    let-exprs (nthrest form 2)
                                    binding-names (take-nth 2 bindings)
                                    binding-vals (map
                                                  #(if (await-seq? %) (second %) %)
                                                  (take-nth 2 (drop 1 bindings)))]
                                {:current (group)
                                 :rest (conj rest
                                             (group (conj forms (list 'js/Promise.all binding-vals)))
                                             (group [(concat ['cljest.helpers.core/async] let-exprs)] binding-names))})

                              ;; If we have `let` but there aren't any `await` calls in the binding values, just create a new
                              ;; `async` wrapped group.
                              (= 'let (first form))
                              {:current (group)
                               :rest (conj rest
                                           (group
                                            (conj forms (list 'let (second form) (concat ['cljest.helpers.core/async] (nthrest form 2))))
                                            bindings))}

                              ;; Otherwise, add the current form to the current group's forms.
                              :else
                              {:current (group (conj forms form) bindings)
                               :rest rest}))
                          {:current (group)
                           :rest []})

                         ;; Prevent unnecessary functions from being added to the result
                         ((fn [{:keys [current rest]}]
                            (if (empty? (:forms current))
                              rest
                              (conj rest current))))
                         (map (fn [{:keys [forms bindings]}]
                                (list '.then (concat (list 'fn (apply vector bindings)) forms)))))]
    `(-> (js/Promise.resolve)
         ~@then-groups)))
