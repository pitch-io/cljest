(ns cljest.compilation.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [io.aviso.ansi :refer [bold bold-blue bold-green bold-red]]
            [malli.core :as malli]
            malli.transform))

(defonce ^:private !config (atom nil))

(def ^:private config-schema
  [:map
   {:closed true}
   [:compiler-options {:optional true} [:map
                                        {:closed true}
                                        [:closure-defines :map]]]
   [:test-src-dirs [:sequential :string]]
   [:ns-suffixes [:sequential {:default ['-test]} :symbol]]
   [:mode [:enum {:error/message "only :all is allowed" :default :all} :all]]
   [:preloads-ns [:symbol {:default 'cljest.preloads}]]])

(defn ^:private read-edn-safely
  "Given a File instance, reads it and attempts to parse as EDN. If it fails, returns nil rather than throwing."
  [io]
  (try
    (-> io
        slurp
        edn/read-string)
    (catch RuntimeException _ nil)))

(defn ^:private pluralize
  "A simple pluralization method that adds an 's' to the end of the provided `word` if `n`
  is not 1."
  [word n]
  (if (= 1 n)
    word
    (str word "s")))

(defn ^:private humanize-error
  "Given a malli schema error, generates a 'pretty' error message."
  [error]
  (let [{:keys [in schema value]} error
        pretty-path (bold (str/join " → " in))
        pretty-value (bold-blue value)
        error-type (:type error)]

    (cond
      (= ::malli/extra-key error-type)
      (str "You added an extra config key: " pretty-path ". Double check the spelling of the key.")

      (and (= :enum (malli/type schema)) (not (contains? (malli/children schema) pretty-value)))
      (str "The value at " pretty-path " did not match any of the allowed values. Value: " pretty-value ". Allowed values: " (bold-green (str/join ", " (malli/children schema))))

      (and (= :string (malli/type schema)) (not (string? value)))
      (str "The value at " pretty-path " should be a string but is not. Value: " pretty-value)

      (and (= :symbol (malli/type schema)) (not (symbol? value)))
      (str "The value at " pretty-path " should be a symbol but is not. Value: " pretty-value)

      :else
      (str "An unexpected error happened while attempting to parse " pretty-path ". Please report this as a bug and include your config in the report."))))

(defn ^:private coerce-config-with-pretty-exception!
  "Coerces the raw config based on the Malli schema. If it fails coercion, a human friendly exception is thrown."
  [raw]
  (try
    (malli/coerce config-schema raw malli.transform/default-value-transformer)
    (catch Exception e
      (let [explanation (ex-data e)
            errors (get-in explanation [:data :explain :errors])
            num-errors (count errors)]
        (throw (Exception. (str (bold-red (str "Error: Your cljest.edn file had " num-errors " " (pluralize "error" num-errors) "."))
                                "\n\n"
                                (str/join "\n" (map humanize-error errors)))))))))

(defn- load-config!
  "Loads the cljest.edn file and coerces based on the Malli schema."
  []
  (let [config-io (io/file "cljest.edn")]
    (when-not (.exists config-io)
      (throw (Exception. (str (bold-red "Error: A cljest.edn should exist in the same directory as you started the cljest server.")
                              " "
                              "This file should be located next to your jest.config.js file (or equivalent)."))))

    (let [raw-config (read-edn-safely config-io)
          config (coerce-config-with-pretty-exception! raw-config)]
      (reset! !config config))))

(defn get-config!
  "Returns the coerced config. If it hasn't been loaded yet, load it."
  []
  (if-not @!config
    (load-config!)
    @!config))
