(ns cljest.matchers)

(defmacro make-matcher
  "Most matchers accept either 0, 1, or 2 arguments. This handles those cases.

  Generates code that looks like this:

  ```
  expect(actual).matcherName()
  expect(actual).matcherName(expected)
  expect(actual).matcherName(expected, secondExpected)

  ```"
  ([matcher-name actual]
   `(make-matcher ~matcher-name ~actual js/undefined js/undefined))

  ([matcher-name actual expected]
   `(make-matcher ~matcher-name ~actual ~expected js/undefined))

  ([matcher-name actual expected second-expected]
   (let [matcher (symbol matcher-name)]
     `(.. (js/expect ~actual) (~matcher ~expected ~second-expected)))))

(defmacro make-optional-matcher
  "Some matchers, like `.toHaveAttribute`, optionally accept an argument (or second argument) to the matcher.

  Generates code like this:

  ```
  expect(actual).matcherName()
  expect(actual).matcherName(maybeValue)

  expect(actual).matcherName(value)
  expect(actual).matcherName(value, maybeExtraValue)
  ```"
  ([matcher-name actual maybe-value]
   `(make-optional-matcher ~matcher-name ~actual ~maybe-value nil))

  ([matcher-name actual value maybe-extra-value]
   (let [matcher (symbol matcher-name)]
     `(cond
        (nil? ~value)
        (.. (js/expect ~actual) (~matcher))

        (nil? ~maybe-extra-value)
        (.. (js/expect ~actual) (~matcher ~value))

        :else
        (.. (js/expect ~actual) (~matcher ~value ~maybe-extra-value))))))
