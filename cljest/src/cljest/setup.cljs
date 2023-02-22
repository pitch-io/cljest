(ns cljest.setup
  "The default setup file for cljest configurations in Jest."
  (:require [applied-science.js-interop :as j]
            lambdaisland.deep-diff2))

(defn- matcher [matcher]
  (fn [& args]
    (clj->js (apply matcher args))))

(defn- jest-fn-called-with
  "Asserts that the provided `spy` was ever called with the provided `args`."
  [spy args]
  (let [calls (j/get-in spy [:mock :calls])
        passing-call? (->> calls
                           (filter #(= args (vec %)))
                           first)]
    (if passing-call?
      {:pass true
       :message #(str "Expected spy not to be called with " args)}
      {:pass false
       :message #(str "Expected spy to be called with " args)})))

(defn- jest-is
  [body-fn formatter-fn]
  (let [value (body-fn)]
    (if (or value (= value js/undefined))
      {:pass true}
      {:pass false
       :message formatter-fn})))

(js/expect.extend #js {"cljest__is" (matcher jest-is)})
(js/expect.extend #js {"customCalledWith" (matcher jest-fn-called-with)})
