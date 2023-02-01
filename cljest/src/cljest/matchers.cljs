(ns cljest.matchers
  (:require-macros [cljest.matchers :refer [make-matcher make-optional-matcher]]))

; jest.fn
(defn called? [spy] (make-matcher "toHaveBeenCalled" spy))
(defn not-called? [spy] (make-matcher "not.toHaveBeenCalled" spy))
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
(defn not-has-class? [element class] (make-matcher "not.toHaveClass" element class))
(defn has-focus? [element] (make-matcher "toHaveFocus" element))
(defn has-style? [element css] (make-matcher "toHaveStyle" element css))
(defn has-text-content? [element text] (make-matcher "toHaveTextContent" element text))
(defn has-value? [element value] (make-matcher "toHaveValue" element value))
(defn has-display-value? [element value] (make-matcher "toHaveDisplayValue" element value))
(defn checked? [element] (make-matcher "toBeChecked" element))
(defn not-checked? [element] (make-matcher "not.toBeChecked" element))
(defn partially-checked? [element] (make-matcher "toBePartiallyChecked" element))
(defn has-error-msg? [element message] (make-matcher "toHaveErrorMessage" element message))

; TODO: how best to handle negative matchers, too? Just copy-and-paste?
(defn has-accessible-description? [element & [expected-desc]] (make-optional-matcher "toHaveAccessibleDescription" element expected-desc))
(defn has-accessible-name? [element & [expected-name]] (make-optional-matcher "toHaveAccessibleName" element expected-name))
(defn has-attr? [element attribute & [value]] (make-optional-matcher "toHaveAttribute" element attribute value))
(defn has-no-attr? [element attribute & [value]] (make-optional-matcher "not.toHaveAttribute" element attribute value))
