#!/usr/bin/env bb

(ns app.options
  (:require [clojure.edn :as edn]
            [babashka.fs :as fs]))

(defn validate-opts [opts]
  (let [error-msg (fn [opt] (println "Error in config file at option " opt ". Please edit 'config.edn'")
                            (System/exit 1))]
    (cond
      (not (int? (:line-length opts)))          (error-msg :line-length)
      (not (boolean? (:headers opts)))          (error-msg :headers)
      (not (#{"" ".txt"}
            (:file-extension-preference opts))) (error-msg :file-extension-preference)
      (not (boolean? (:overwrite opts)))        (error-msg :overwrite)
      :else                                     opts)))

(def options
  (if (fs/exists? "config.edn") (->> (slurp "config.edn")
                                     edn/read-string
                                     validate-opts)
      {:line-length               70
       :headers                   false
       :file-extension-preference ""
       :overwrite                 false}))
(ns app.rellinks
  (:require [babashka.fs    :as fs]
            [clojure.string :as str]
            [app.options :refer [options]]))

(defn parent-directories
  "Returns path components for file as list (excluding root, i.e. gemlog-directory)
  gemlog-directory is the full path to the gemlog-directory"
  [file-path gemlog-directory]
  (mapv str (fs/components (str/replace file-path gemlog-directory ""))))

(defn missing-parents [uri-parents file-parents]
  (let [depth       (- (count file-parents)
                       (count (filter #{".."} uri-parents)))
        replacement (take depth file-parents)
        cleaned     (remove #{".."} uri-parents)]
    (str/join "/" (flatten (conj cleaned replacement)))))

(defn translate-file-extension [uri]
  (str/replace uri #".gmi" (:file-extension-preference options)))

(defn convert-relative-links [uri file gemlog-directory domain]
  (let [parents-uri (parent-directories uri gemlog-directory)
        parents-file (parent-directories file gemlog-directory)
        domain (str domain "/")]
    (if (some #{".."} parents-uri)
      (str "gopher://" domain "0/"
           (translate-file-extension
            (missing-parents parents-uri parents-file)))
      (str "gopher://" domain "0/"
           (translate-file-extension
            (str/join "/" (concat (butlast parents-file) parents-uri)))))))


(ns app.header
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [app.options :refer [options]]))

(defn word-count [file]
  (let [split (str/split (slurp file) #"\W+")]
    (->> split
         (filter #(re-find #"[A-Za-z]" %))
         (count))))

(defn title [file]
  (let [line1 (first (fs/read-all-lines file))]
    (when line1 (str/replace line1 #"^#+?\s" ""))))

(defn date [file]
  (let [filename-date (re-find #"\d{4}-\d{2}-\d{2}" file)]
    (if filename-date filename-date
        (let [ctime (.toString (fs/creation-time file))]
          (subs ctime 0 10)))))

(defn make-header [file]
  (let [border (str/join (repeat (:line-length options) "="))]
    (str
     border "\n"
     (str "Title:      " (title file))      "\n"
     (str "Date:       " (date file))       "\n"
     (str "Word Count: " (word-count file)) "\n"
     border "\n\n\n")))
(ns app.linewrap
  (:require [clojure.string :as str]
            [app.options :refer [options]]))


(defn index-of-spaces [line]
  (let [m (re-matcher #" " line)]
    ((fn step []
       (when (. m find)
         (cons (. m start)
               (lazy-seq (step))))))))

(defn split-lines-at-limit [line]
  (let [line-length (:line-length options)]
    (if (> (count line) line-length)
      (let [split-point (->> line
                             (index-of-spaces)
                             (sort-by #(Math/abs (- line-length %)))
                             (first))]
        (str (str (subs line 0 split-point) "\n")
             (split-lines-at-limit (subs line (inc split-point)))))
      line)))

(defn wrap-line [line]
  (str/replace
   (split-lines-at-limit line)
   #"\n\s+" "\n"))
(ns app.convert
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [app.linewrap :refer [wrap-line]]
            [app.options :refer [options]]
            [app.header :refer [make-header]]
            [app.rellinks :refer [convert-relative-links]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Linetypes

(def gemini-linetypes
  {:h1            #"^# "
   :h2            #"^## "
   :h3            #"^### "
   :link          #"^=> "
   :quote         #"^> "
   :list          #"^\* "
   :pre           #"^```"
   :my-pre-marked #":pre "})

(defn mark-preformatted-lines
  "not the best fn, but the point here is to tell the converters later to ignore lines marked as ':pre'"
  [lines]
  (loop [lines  lines
         pre    :OFF
         marked []]
    (if (empty? lines) marked
        (let [line (first lines)
              type (re-find (:pre gemini-linetypes) line)]
          (if (= pre :ON)
            (recur (rest lines) (if type :OFF :ON)
                   (conj marked (if type line (str ":pre " line))))
            (recur (rest lines) (if type :ON :OFF)
                   (conj marked line)))))))

(defn determine-linetype [gemtext-line]
  (let [[type _]
        (->> gemini-linetypes
             (filter (fn [[k v]]
                       (when (re-find v gemtext-line) k)))
             first)]
    (or type :normal-line)))


(defn assign-linetype [gemtext-file]
  (let [lines (fs/read-all-lines gemtext-file)
        marked (->> lines
                    mark-preformatted-lines
                    (map determine-linetype))]
    [marked lines]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Conversion Functions


(defn convert-headings [line level]
  (let [title   (str/replace line #"^#+?\s" "")
        length  (count title)]
    (if (= level :h3) (str "-" title "-")
        (str title "\n"
         (str/join (repeat length (if (= level :h1) "=" "-")))))))

(defn convert-quote [line]
 (str/replace (wrap-line line) #"\n" "\n> "))

(defn convert-list [line]
  (let [lines (wrap-line line)]
    (str/replace lines #"\n" "\n  ")))

(defn handle-pre [line]
  (str/replace line #":pre " ""))

(defn internal-link?
  "Regex copied from online, there has to be a better way to do this..."
  [line]
  (not (re-find #"^(gemini|https|http|gopher|ftp|git):\/\/(?:www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b(?:[-a-zA-Z0-9()@:%_\+.~#?&\/=]*)$" line)))

(defn convert-link [line file gemlog-dir domain]
  (let [[_ uri & label] (str/split line #" ")
        make-link       (fn [uri label]
                          (str "~ " (str/join " " label) ":"
                               "\n"
                               uri))]
    (if (internal-link? uri)
      (let [converted-uri (convert-relative-links uri file gemlog-dir domain)]
        (make-link converted-uri label))
      (make-link uri label))))

(defn convert-line
  "The 'file' 'gemlog-dir' 'domain' are only needed here for cases of relative links"
  [type line file gemlog-dir domain]
  (case type
    :h1            (convert-headings line :h1)
    :h2            (convert-headings line :h2)
    :h3            (convert-headings line :h3)
    :link          (convert-link line file gemlog-dir domain)
    :quote         (convert-quote line)
    :list          (convert-list line)
    :pre           line
    :my-pre-marked (handle-pre line)
    :normal-line   (wrap-line line)))

(defn convert-file
  [gemtext-file output-file capsule-dir domain]
  (let [[types lines] (assign-linetype gemtext-file)]
    (io/make-parents output-file)
    (spit output-file
          (str
           (when (:headers options)
             (str (make-header gemtext-file)))
           (str/join "\n"
                     (map #(convert-line %1 %2
                                         gemtext-file
                                         capsule-dir
                                         domain)
                          types
                          lines))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Categorize and map files

(defn file-parent-dirs
  "The path to file (from capsule root)"
  [file capsule-dir]
  (str/replace (str (fs/parent file)) capsule-dir ""))

(defn convert-path
  "returns new path for file, which will be written to after conversion."
  [file capsule-directory destination-directory]
  (let [ext (:file-extension-preference options)]
    (str
     (str (fs/file destination-directory)) ;; the fs/file cooercion here removes trailing slashes
     (file-parent-dirs file (str (fs/file capsule-directory)))
     "/"
     (if (re-find #".gmi" file)
       (str (fs/strip-ext (fs/file-name file)) ext)
       (fs/file-name file)))))

(defn sort-files [capsule-dir]
  (let [all-files (map str (fs/glob capsule-dir "**"))]
    (reduce (fn [[regular-list gemtext-list] file]
              (cond
                (fs/directory? file) [regular-list gemtext-list]
                (re-find #".gmi" file)[regular-list (conj gemtext-list file)]
                :else [(conj regular-list file) gemtext-list]))
            [[] []]
            all-files)))

(defn prepare-files [capsule-dir destination-dir]
  (let [[regular gemtext] (sort-files capsule-dir)
        reg-to            (map #(convert-path % capsule-dir destination-dir) regular)
        gemtext-to        (map #(convert-path % capsule-dir destination-dir) gemtext)]
    {:regular-files regular
     :regular-to    reg-to
     :gemtext       gemtext
     :gemtext-to    gemtext-to}))

(defn copy-reg-files [file destination]
  (io/make-parents destination)
  (fs/copy file destination))


(defn capsule->gopherhole [capsule-dir output-dir domain]
  (let [{:keys [regular-files regular-to gemtext gemtext-to]}
        (prepare-files capsule-dir output-dir)]
    (dorun (map #(convert-file %1 %2 capsule-dir domain) gemtext gemtext-to))
    (dorun (map #(copy-reg-files %1 %2) regular-files regular-to))))




(require '[babashka.cli :as cli])
(require '[babashka.fs :as fs])
(require '[app.convert :refer [convert-file capsule->gopherhole]])

(def capsule-spec {:capsule {:ref   "<directory>"
                             :desc  "The directory you wish to covert to a gopherhole"
                             :alias :c}
                   :output  {:ref          "<directory>"
                             :desc         "The destination directory for the conversion"
                             :alias        :o
                             :default-desc "gemlog_gopherhole"}
                   :domain  {:ref          "<domain>"
                             :desc         "Domain name for serving the new gopherhole"
                             :alias        :d
                             :default-desc "localhost"
                             :default      "localhost"}})

(def single-spec {:file   {:ref   "<file>"
                           :desc  "The gemtext file you wish to convert"
                           :alias :f}
                  :output {:ref         "<destination>"
                           :alias       :o
                           :defaul-desc "file_gopher.txt"}
                  :domain {:ref          "<domain>"
                           :alias        :d
                           :desc         "Domain name (for converting relative links)"
                           :default-desc "localhost"
                           :default      "localhost"}})

(defn help
  [_]
  (println
   (str "Usage:\n"
    "capsulego capsule \n"
    (cli/format-opts {:spec capsule-spec :order [:capsule :output :domain]})
    "\n"
    "capsulego file \n"
    (cli/format-opts {:spec single-spec :order [:file :output :domain]}))))


;; TODO currently 'gemlog' is set to current working directory, would be better to just have this as optional in single covert
(defn convert-single
  [{:keys [opts]}]
  (if (and (:file opts) (fs/exists? (:file opts)))
      (convert-file
       (:file opts)
       (or (:output opts) (str (fs/strip-ext (fs/absolutize (:file opts))) "_gopher.txt"))
       (str (last (fs/components (fs/cwd))))
       (or (:domain opts) "localhost"))
      (help opts)))


(defn convert-capsule
  [{:keys [opts]}]
  (cond
    (or (nil? (:capsule opts))
        (not (fs/exists? (:capsule opts)))) (do (println "No capsule directory provided...") (help opts))
    (and (not (nil? (:output opts)))
         (false? (:overwrite opts))
         (fs/exists? (:output opts)))       (do (println "Output directory already exists, aborting...") (System/exit 1))
    :else
    (capsule->gopherhole
     (:capsule opts)
     (or (:output opts) (str (fs/absolutize (:capsule opts)) "_gopher"))
     (or (:domain opts) "localhost"))))



(def table
  [{:cmds ["capsule"] :fn convert-capsule :spec capsule-spec}
   {:cmds ["file"] :fn convert-single :spec single-spec}
   {:cmds [] :fn help}])


(cli/dispatch table *command-line-args*)




