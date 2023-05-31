(ns single.convert
  (:require [babashka.fs :as fs]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [single.lines :as l]
            [single.linewrap :as wrap]
            [single.header :as header]
            [gophermap.lines :as gmap]
            [gophermap.links :as glinks]))
  
(defn gemtext-linetype [gemtext-line]
  (loop [types    (keys l/gemini-linetypes)
         matchers (vals l/gemini-linetypes)]
    (if (empty? matchers) :normal-line
        (let [matcher (first matchers)
              type    (first types)]
          (if (re-find matcher gemtext-line) type
              (recur
               (rest types)
               (rest matchers)))))))

(defn type-map [gemtext]
  (let [types (map #(gemtext-linetype %) gemtext)]
    [types gemtext]))

(defn convert-line [type line file]
  (case type
    :h1             (l/convert-headings line :h1)
    :h2             (l/convert-headings line :h2)
    :h3             (l/convert-headings line :h3)
    :link           (l/convert-link line file)
    :quote          (l/convert-quote line)
    :list           (l/convert-list line)
    :pre            line
    :my-pre-marked  (l/handle-pre line)
    :normal-line    (wrap/wrap-line line)))

(defn convert-index-line [type line file]
  (case type
    :h1             (gmap/gm-headings line :h1)
    :h2             (gmap/gm-headings line :h2)
    :h3             (gmap/gm-headings line :h3)
    :link           (glinks/gmap-link line file)
    :quote          (gmap/gm-quote line)
    :list           (gmap/gm-list line)
    :pre            ""
    :my-pre-marked  (gmap/gm-pre line)
    :normal-line    (gmap/gm-normal-line line)))


(defn convert-file [gemtext-file output-file header?]
  (let [lines   (fs/read-all-lines gemtext-file)
        marked  (type-map (l/mark-preformatted-lines lines))]
    (io/make-parents output-file)
    (spit output-file
          (str
           (when (= :headers header?)
             (str (header/make-header gemtext-file)
                  "\n\n"))
           (s/join "\n"
                   (map #(convert-line %1 %2 gemtext-file)
                        (first marked)
                        (second marked)))))))



(defn convert-index-file [index-file output-file]
  (let [lines   (fs/read-all-lines index-file)
        marked  (type-map (l/mark-preformatted-lines lines))]
    (io/make-parents output-file)
    (spit output-file
          (s/join "\n"
                  (map #(convert-index-line %1 %2 index-file)
                       (first marked)
                       (second marked))))))
