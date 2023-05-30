(ns cljest.matchers)

(defmacro defmatcher
  "A macro for defining a Jest matcher. Creates a function with metadata that will allow
  `cljest.core/is` to treat this symbol as a Jest matcher, rather than a regular symbol.

  This allows the compiler to generate simpler code, making one expect call for the matcher,
  rather than two (the `is` and the underlying matcher).

  When the function defined by `defmatcher` is called, it will throw as it is replaced when
  compiled in `is`."
  [sym matcher-name]
  `(defn ~(with-meta sym {:jest-matcher matcher-name}) [& _#]
     (throw (ex-info (str "You must call " ~(str sym) " inside of `cljest.core/is`.") {:matcher-name ~matcher-name}))))
