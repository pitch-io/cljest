(ns cljest.matchers
  (:require [applied-science.js-interop :as j]
            [cljest.core]))

(defn make-matcher
  "Most matchers accept either 0, 1, or 2 arguments. This handles those cases.

  Used for matchers that follow these patterns:

  ```
  expect(actual).matcherName()
  expect(actual).matcherName(expected)
  expect(actual).matcherName(expected, secondExpected)

  ```
  "
  ([name actual]
   (make-matcher name actual js/undefined))
  ([name actual expected]
   (make-matcher name actual expected js/undefined))
  ([name actual expected second-expected]
   (if-not cljest.core/*inside-is?*
     (throw (ex-info (str "You must call " name " inside of `cljest.core/is`.") {}))
     (let [raw-expect-call (js/expect actual)
           expect-call (if cljest.core/*is-body-negated?*
                         (j/get raw-expect-call :not)
                         raw-expect-call)]
       (j/call expect-call name expected second-expected)

       ;; So that `is` will pass.
       true))))

(defn make-optional-matcher
  "Some matchers, like `.toHaveAttribute`, optionally accept an argument (or second argument) to the matcher,
  and can't take js/undefined -- the argument must be unprovided.

  Used for matchers that follow these patterns:

  ```
  expect(actual).matcherName()
  expect(actual).matcherName(maybeValue)

  expect(actual).matcherName(value)
  expect(actual).matcherName(value, maybeExtraValue)
  ```"
  ([name actual maybe-value]
   (make-optional-matcher name actual maybe-value nil))

  ([name actual value maybe-extra-value]
   (if-not cljest.core/*inside-is?*
     (throw (ex-info (str "You must call " name " inside of `cljest.core/is`.") {}))
     (let [raw-expect-call (js/expect actual)
           expect-call (if cljest.core/*is-body-negated?*
                         (j/get raw-expect-call :not)
                         raw-expect-call)]
       (cond
         (nil? value)
         (j/call expect-call name)

         (nil? maybe-extra-value)
         (j/call expect-call name value)

         :else
         (j/call expect-call name value maybe-extra-value))
       true))))

; jest.fn
(defn called? [spy] (make-matcher "toHaveBeenCalled" spy))
(defn called-times? [spy n] (make-matcher "toHaveBeenCalledTimes" spy n))
(defn called-with? [spy & args] (make-matcher "customCalledWith" spy args))

; jest-dom
(defn disabled? [element] (make-matcher "toBeDisabled" element))
(defn enabled? [element] (make-matcher "toBeEnabled" element))
(defn empty-dom-element? [element] (make-matcher "toBeEmptyDOMElement" element))
(defn in-the-document? [element] (make-matcher "toBeInTheDocument" element))
(defn invalid? [element] (make-matcher "toBeInvalid" element))
(defn required? [element] (make-matcher "toBeRequired" element))
(defn valid? [element] (make-matcher "toBeValid" element))
(defn visible? [element] (make-matcher "toBeVisible" element))
(defn contains-element? [element descendent] (make-matcher "toContainElement" element descendent))
(defn contains-html? [expected actual] (make-matcher "toContainHTML" actual expected))
(defn has-attribute? [element attribute value] (make-matcher "toHaveAttribute" element attribute value))
(defn has-class? [element class & [options]] (make-optional-matcher "toHaveClass" element class options))
(defn has-focus? [element] (make-matcher "toHaveFocus" element))
(defn has-style? [element css] (make-matcher "toHaveStyle" element css))
(defn has-text-content? [element text] (make-matcher "toHaveTextContent" element text))
(defn has-value? [element value] (make-matcher "toHaveValue" element value))
(defn has-display-value? [element value] (make-matcher "toHaveDisplayValue" element value))
(defn checked? [element] (make-matcher "toBeChecked" element))
(defn partially-checked? [element] (make-matcher "toBePartiallyChecked" element))
(defn has-error-msg? [element message] (make-matcher "toHaveErrorMessage" element message))
(defn has-accessible-description? [element & [expected-desc]] (make-optional-matcher "toHaveAccessibleDescription" element expected-desc))
(defn has-accessible-name? [element & [expected-name]] (make-optional-matcher "toHaveAccessibleName" element expected-name))
(defn has-attr? [element attribute & [value]] (make-optional-matcher "toHaveAttribute" element attribute value))
