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
