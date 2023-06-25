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

(defn format-link [uri & label]
  (if label (str "~ "
                 (str/join " " label) ":"
                 "\n  "
                 uri)
      (str "~ " uri)))
    
(defn convert-link [line file gemlog-dir domain]
  (let [[_ uri & label] (str/split line #" ")]
    (if (internal-link? uri)
      (let [converted-uri (convert-relative-links uri file gemlog-dir domain)]
        (format-link converted-uri label))
      (format-link uri label))))


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


