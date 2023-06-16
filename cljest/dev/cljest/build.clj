(ns cljest.build
  (:require [clojure.data.xml :as xml]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as tools.build.api]
            [clojure.tools.build.tasks.write-pom :as tools.build.tasks.write-pom]))

;; Adapted from https://github.com/mentat-collective/Mafs.cljs/blob/60a074ee8720c3252f1364cf8ea7a3fdac3e6c87/build.clj#L7-L15
(xml/alias-uri 'pom "http://maven.apache.org/POM/4.0.0")

(alter-var-root
 #'tools.build.tasks.write-pom/to-dep
 (fn [orig]
   ;; This isn't really clean. `to-dep` seems to drop `original?` because of the `?`,
   ;; but that's an implementation detail. Since this whole thing is a hack it's
   ;; "fine".
   (fn [[_ {:keys [optional?]} :as pair]]
     (cond-> (orig pair)
       optional?
       (conj [::pom/scope "provided"])))))

(defn get-build-config
  "Reads the build config from `build.edn`. Assumes you execute whatever uses this
  in the same pwd as where the file lives."
  []
  (-> "build.edn"
      io/file
      slurp
      edn/read-string))

(defn ^:private lib->group-id
  [lib]
  (-> lib
      str
      (str/split #"/")
      first))

(defn jar-path
  "Returns the built JAR path. Throws if a JAR for the current build version
  doesn't exist."
  []
  (let [{:keys [lib version]} (get-build-config)
        raw-file (io/file (str "dist/" lib "-" version ".jar"))]
    (if (.exists raw-file)
      (.getCanonicalPath raw-file)
      (throw (ex-info (str "JAR file for cljest " version " does not exist. Ensure that cljest has been built.") {})))))

(defn pom-path
  "Returns the generated POM path. Throws if a POM for the current build version
  doesn't exist."
  []
  (let [{:keys [lib]} (get-build-config)
        group-id (lib->group-id lib)
        raw-file (io/file (str "dist/" group-id "/pom.xml"))]
    (if (.exists raw-file)
      (.getCanonicalPath raw-file)
      (throw (ex-info (str "POM file for cljest does not exist. Ensure that cljest has been built.") {})))))

(defn main
  [& _]
  (let [{:keys [version optional-deps lib extra-paths ignore src-dirs]} (get-build-config)
        basis (tools.build.api/create-basis {:project "deps.edn"
                                             ;; `optional-deps` are still added to the POM, so that tools such as
                                             ;; `cljdoc-analyzer` can work correctly, but are marked as "provided",
                                             ;; meaning they are "provided" by the user. This isn't supported in
                                             ;; `deps.edn` or in `write-pom` (yet?), so until it is supported we need
                                             ;; to hack around it.
                                             ;;
                                             ;; This is hacky for a couple of reasons. 1. The obvious: we need to alter
                                             ;; `tools.build.tasks.write-pom/to-dep` to understand `optional?`; and,
                                             ;; 2. the `optional?` is implicitly dropped from the POM generation because
                                             ;; POM properties aren't munged and instead silently dropped at some point if
                                             ;; they can't be serialized.
                                             ;;
                                             ;; I think this is okay since whatever the official solution is will likely
                                             ;; require some change anyway, so it's fine to be hacky here.
                                             ;;
                                             ;; See https://ask.clojure.org/index.php/9110 and https://ask.clojure.org/index.php/12817.
                                             :extra {:deps (->> optional-deps
                                                                (map (fn [[k v]]
                                                                       [k (assoc v :optional? true)]))
                                                                (into {}))}})
        jar-dir "dist/build_files/jar"
        pom-dir (str "dist/" (lib->group-id lib))]
    (tools.build.api/copy-dir {:src-dirs src-dirs
                               :target-dir jar-dir
                               :ignores (map re-pattern ignore)})

    (doseq [path extra-paths]
      (let [instance (io/file path)
            dir? (.isDirectory instance)]
        (if dir?
          (tools.build.api/copy-dir {:target-dir (str jar-dir "/" (.getName instance))
                                     :src-dirs [path]})
          (tools.build.api/copy-file {:target (str jar-dir "/" (.getName instance))
                                      :src path}))))

    (tools.build.api/jar {:class-dir jar-dir
                          :jar-file (str "dist/" lib "-" version ".jar")})

    (tools.build.api/delete {:path "dist/build_files"})

    (tools.build.api/write-pom {:target pom-dir
                                :lib lib
                                :version version
                                :basis basis
                                :src-dirs ["src"]})))

