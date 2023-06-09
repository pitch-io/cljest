(ns cljest.format)

(defn value->str
  "Translates a value to a string. Handles stringifying nil to 'nil'"
  [value]
  (cond
    (nil? value) "nil"
    :else value))

(defmulti formatter
  (fn [resolved-symbol _ _]
    resolved-symbol))

(defmethod formatter 'cljs.core/nil?
  [_ form negated?]
  (let [a (nth form 1)]
    `(fn []
       (str "Expected " ~(value->str a) " to " ~(when negated? "not ") "be nil."))))

(defmethod formatter 'cljs.core/=
  [_ form negated?]
  (let [a (nth form 1)
        b (nth form 2)]
    `(fn []
       (str "Expected " ~(value->str a) " to " ~(when negated? "not ") "equal " ~(value->str b) "."
            (when-not ~negated?
              (str "\n\n" (cljest.auxiliary/generate-diff ~a ~b)))))))

(defmethod formatter 'cljs.core/not=
  [_ form negated?]
  (let [a (nth form 1)
        b (nth form 2)]
    `(fn []
       (str "Expected " ~(value->str a) " to " ~(when-not negated? "not ") "equal " ~(value->str b) "."
            (when ~negated?
              (str "\n\n" (cljest.auxiliary/generate-diff ~a ~b)))))))

(defmethod formatter :default
  [_ form negated?]
  `(fn []
     (str "Expected " '~form " to " ~(when negated? "not ") "be truthy.\n\n")))
