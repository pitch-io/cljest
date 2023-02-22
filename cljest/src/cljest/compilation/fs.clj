(ns cljest.compilation.fs
  (:require [cljest.compilation.config :as config]
            [clojure.core.async :as as]
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

(defn poll-watcher-for-changes
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

(defn ^:private poll-for-all-watchers-changes
  "For all watchers, poll for any changes, returning a flattened vector of all changes."
  []
  (->> @watchers
       (map poll-watcher-for-changes)
       flatten))

(defn get-latest-changes!
  "Gets the latest changes from the watchers. Will wait up to 500ms for the first changes from any
  watcher, and if no changes occur within 500ms, returns nil.

  Jest is sometimes a bit faster than our internal FileWatcher instance(s) and may call the API before
  any changes are detected. To avoid a race condition, which would result in nothing being picked up
  for compilation and Jest having out of date test code, a 500ms timeout is added, which should be more
  than enough to pick up any changes in this specific case."
  []
  (let [stop-chan (as/chan)
        watch-loop (as/go-loop []
                     (let [changes (poll-for-all-watchers-changes)]
                       (if (seq changes)
                         changes
                         (as/alt!
                           stop-chan nil
                           (as/timeout 10) (recur)))))
        [changes] (as/alts!! [(as/timeout 500) watch-loop])]

    (when-not changes
      (as/put! stop-chan true))

    changes))

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
