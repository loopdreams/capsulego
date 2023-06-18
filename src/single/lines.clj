(ns single.lines
  (:require [clojure.string :as s]
            [single.linewrap :refer [wrap-line]]
            [single.rellinks :as rel]))

(def gemini-linetypes
  {:h1      #"^# "
   :h2      #"^## "
   :h3      #"^### "
   :link    #"^=> "
   :quote   #"^> "
   :list    #"^\* "
   :pre     #"^```"
   :my-pre-marked #":pre "})

(defn mark-preformatted-lines [lines]
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

(defn convert-headings [line level]
  (let [title   (s/replace line #"^#+?\s" "")
        length  (count title)]
    (if (= level :h3) (str "-" title "-")
        (str title "\n"
         (s/join (repeat length (if (= level :h1) "=" "-")))))))


(defn internal-link?
  "Regex copied from online, there has to be a better way to do this..."
  [line]
  (not (re-find #"^(gemini|https|http|gopher|ftp|git):\/\/(?:www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b(?:[-a-zA-Z0-9()@:%_\+.~#?&\/=]*)$" line)))

(defn convert-link [line file]
  (let [[_ uri & label] (s/split line #" ")
        make-link       (fn [uri label]
                          (str "~ " (s/join " " label) ":"
                               "\n"
                               uri))]
    (if (internal-link? uri)
      (let [converted-uri (rel/convert-relative-links uri file)]
        (make-link converted-uri label))
      (make-link uri label))))
        

;; TODO check if this is working properly
(defn convert-quote [line]
  (let [lines (wrap-line line)]
    (s/replace lines #"\n" "\n> ")))

(defn convert-list [line]
  (let [lines (wrap-line line)]
    (s/replace lines #"\n" "\n  ")))

(defn handle-pre [line]
  (s/replace line #":pre " ""))

