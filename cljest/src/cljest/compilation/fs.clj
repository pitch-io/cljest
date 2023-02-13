(ns cljest.compilation.fs
  (:require [cljest.compilation.config :as config]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [shadow.cljs.util]
            [taoensso.timbre :as log])
  (:import [shadow.util FileWatcher]))

(defonce ^:private watchers (atom nil))

(defn ^:private teardown-watchers! []
  (log/infof "Tearing down watchers.")
  (for [[_ watcher] @watchers]
    (.close watcher)))

(defn setup-watchers! []
  (let [{:keys [test-src-dirs]} (config/get-config!)
        watchers-map (->> test-src-dirs
                          (map #(io/file %))
                          (map (fn [dir]
                                 [dir (FileWatcher/create dir ["cljs" "cljc" "clj"])]))
                          (into {}))]
    (reset! watchers watchers-map)

    (.addShutdownHook (Runtime/getRuntime) (Thread. teardown-watchers!))

    @watchers))

(defn get-changes-for-watcher
  [[dir watcher]]
  (if-let [changes (.pollForChanges watcher)]
    (reduce
     (fn [acc [name event]]
       (let [file (io/file dir name)]
         (if (and (not= event :del) (zero? (.length file)))
           acc
           (conj acc {:file file
                      :event event
                      :name name
                      :dir dir
                      :ext (when-let [x (str/last-index-of name ".")]
                             (subs name (inc x)))}))))
     []
     changes)
    []))

(defn get-latest-changes []
  (let [change-events (->> @watchers
                           (map get-changes-for-watcher)
                           flatten)]

    change-events))

(defn ^:private get-test-files-for-dir
  [suffixes dir]
  (->> dir
       (clojure.java.io/file)
       (file-seq)
       (filter #(.isFile %))
       (map str)
       (map #(str/replace-first % (str dir "/") ""))
       (map shadow.cljs.util/filename->ns)
       (map str)
       (filter #(some (fn [suffix] (str/ends-with? % (str suffix))) suffixes))
       (map symbol)))

(defn get-test-files-from-src-dirs
  []
  (let [{:keys [test-src-dirs ns-suffixes]} (config/get-config!)]
    (->> test-src-dirs
         (map (partial get-test-files-for-dir ns-suffixes))
         flatten)))
