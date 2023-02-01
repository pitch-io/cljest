(ns cljest.helpers.dom
  (:refer-clojure :exclude [type])
  (:require ["@testing-library/react" :as rtl]
            ["@testing-library/user-event" :as user-event]
            [applied-science.js-interop :as j]
            [re-frame.core]
            [re-frame.subs]
            [re-frame.trace]
            [reagent.core :as r]))

(defn fire-event
  "Fires an event on the provided element. This is a low level function for non user triggered events.
   Use click/type/keyboard for user triggered events."
  [element event]
  (rtl/fireEvent element event))

(defn click
  "Simulates click on the provided element.

  Example:

  ```
  (let [my-element (h/get-by :test-id \"some-id\")]
    (click my-element))
  ```"
  [element]
  (user-event/default.click element))

(defn upload
  "Change a file input as if a user clicked it and selected files in the resulting file upload dialog.

  Example:

  ```
   (let [buffer (fs/readFileSync (path/join js/__dirname \" __fixtures__ \" \" filename.png \"))
                file (js/File. buffer #js {:type \" image/png \"})]
     (p/do!
      ...
      (h.dom/upload (h.dom/get-by :test-id \" upload-avatar \") file)                                                  
  ```"
  [element files]
  (user-event/default.upload element files))

(defn right-click
  "Simulates right-click on the provided element.

  Example:

  ```
  (let [my-element (h/get-by :test-id \"some-id\")]
    (right-click my-element))
  ```"
  [element]
  (user-event/default.click element #js {:button 2}))

(defn context-menu
  "Simulates context-menu event on `element`."
  [element]
  (rtl/fireEvent.contextMenu element))

(defn clear
  "Simulates clear event on `element`."
  [element]
  (user-event/default.clear element))

(defn keyboard
  "Simulates keyboard events described by `text`.

   See https://testing-library.com/docs/ecosystem-user-event#keyboardtext-options."
  [text]
  (user-event/default.keyboard text))

(defn act
  "Wrap the provided `cb` in `react-dom/test-utils` `act` function. When performing an action on a component
  that may have a side effect that is outside of the normal set of functions provided here, such as firing
  a localStorage update that then updates a component, you should wrap the function in `act`.

  You may also need to wrap the provided helpers in this file in the event that they result in a side effect.

  Example:

  ```
  (act #(js/localStorage.setItem \"ls-key\" true))
  ```"
  [cb]
  (rtl/act (fn []
             (cb)
             js/undefined)))

(defn hover
  "Simulates hover on the provided element.

  Example:

  ```
  (let [my-element (h/get-by :test-id \"some-id\")]
    (hover my-element))
  ```"
  [element]
  (user-event/default.hover element))

(defn type
  "Simulates typing on the provided element (generally an input).

  Example:

  ```
  (type (h/get-by :test-id \"email\") \"john@example.com\")
  ```"
  [element text]
  (user-event/default.type element text))

(defn wait-for+
  "Calls the provided `cb` until it doesn't throw. Returns a promise.

  Useful when you've performed an action on an element (e.g. clicking) and are now waiting for a matcher to pass.

  Example:

  ```
  (p/do!
     (click (h/get-by :test-id \"some-id\"))
     (wait-for+ #(m/visible? (h/get-by :test-id \"some-other-id\")))
     (click (h/get-by :test-id \"some-other-id\")))
  ```"
  [cb]
  (rtl/waitFor cb))

(defn debug
  "Pretty prints the HTML of the screen, or given `ele` if provided, using console.log."
  ([] (.debug rtl/screen))
  ([ele] (.debug rtl/screen ele)))

(defn within
  "Gets an element within another element. Useful in cases like getting an element that has the same test ID or text
  as another but is inside of a distinct parent.

  Example:

  ```
  (let [rows (h/get-all-by :test-id \"table-row\")
        row (get rows 1)]
    (m/visible? (h/get-by :test-id \"row-name\" (within row))))
  ```"
  [ele]
  (rtl/within ele))

(defn render
  "Takes a component and renders it. If it is a reagent component it will be re-rendered when an atom
  updates.

  Does not return anything, and you should use queries like `get-by`, `query-by`, `get-all-by`
  to get elements on the screen.

  Example:

  ```
  (render [my-cool-component {:best-prop true}])
  ```"
  [component]

  (let [as-element #(r/as-element component)
        rendered (rtl/render (as-element))
        rerender (.-rerender rendered)]
    (r/after-render #(rerender (as-element)))))

(defn- query
  "Queries for an element on the screen based on the given `query-name`. Used by the multimethods
 like `get-by` and `query-by`."
  [query-name selector maybe-scoped-ele]
  (let [ele (or maybe-scoped-ele rtl/screen)]
    (j/call ele query-name selector)))

(defmulti get-by
  "Gets an element using the specified type and selector. May optionally take a scope Testing
  Library element. Throws if the element cannot be found or if more than one element was found.

  See https://testing-library.com/docs/queries/about#types-of-queries

  Example:

  ```
  (get-by :test-id \"my-ele-test-id\")
  (get-by :text \"some button text\")

  (let [scoped-ele (within (get-by :test-id \"my-ele\"))]
    (get-by :text \"A name\" scoped-ele))
  ```"
  (fn [by _ & _] by))

(defmethod get-by :test-id [_ selector & [scoped]] (query :getByTestId selector scoped))
(defmethod get-by :text [_ selector & [scoped]] (query :getByText selector scoped))
(defmethod get-by :role [_ selector & [scoped]] (query :getByRole selector scoped))
(defmethod get-by :placeholder-text [_ selector & [scoped]] (query :getByPlaceholderText selector scoped))
(defmethod get-by :label-text [_ selector & [scoped]] (query :getByLabelText selector scoped))
(defmethod get-by :title [_ selector & [scoped]] (query :getByTitle selector scoped))

(defmulti get-all-by
  "Gets all elements using the specified type and selector. May optionally take a scope Testing
  Library element. Throws if no elements could be found.

  See https://testing-library.com/docs/queries/about#types-of-queries

  Example:

  ```
  (get-all-by :test-id \"my-ele-test-id\")
  (get-all-by :text \"some button text\")
  ```"
  (fn [by _ & _] by))
(defmethod get-all-by :test-id [_ selector & [scoped]] (query :getAllByTestId selector scoped))
(defmethod get-all-by :text [_ selector & [scoped]] (query :getAllByText selector scoped))
(defmethod get-all-by :role [_ selector & [scoped]] (query :getAllByRole selector scoped))

(defmulti query-by
  "Queries an element by the specified type and selector. Returns null if no elements are found, and
  throws if more than one element is found. May optionally take a scope Testing Library element.

  See https://testing-library.com/docs/queries/about#types-of-queries

  Example:

  ```
  (query-by :test-id \"a-test-id\")
  (query-by :text \"some text\")
  ```"
  (fn [by _ & _] by))
(defmethod query-by :test-id [_ selector & [scoped]] (query :queryByTestId selector scoped))
(defmethod query-by :text [_ selector & [scoped]] (query :queryByText selector scoped))
(defmethod query-by :role [_ selector & [scoped]] (query :queryByRole selector scoped))
(defmethod query-by :label-text [_ selector & [scoped]] (query :queryByLabelText selector scoped))
(defmethod query-by :placeholder-text [_ selector & [scoped]] (query :queryByPlaceholderText selector scoped))
