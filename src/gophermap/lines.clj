(ns gophermap.lines
  (:require [single.lines :as l]
            [clojure.string :as s]
            [single.linewrap :as wrap]))


(defn gm-i-conversions [line fn & level]
  (let [lines (s/split-lines (if level
                               (fn line (first level))
                               (fn line)))]
    (->> lines
         ;; (map #(str "i" %))
         (map #(str %))
         (s/join "\n"))))

(defn gm-normal-line [line]
  (gm-i-conversions line wrap/wrap-line))

(defn gm-headings [line level]
  (gm-i-conversions line l/convert-headings level))

(defn gm-quote [line]
  (gm-i-conversions line l/convert-quote))

(defn gm-list [line]
  (gm-i-conversions line l/convert-list))

(defn gm-pre [line]
  (gm-i-conversions line l/handle-pre))

(defn gm-link-format [uri label]
  (->>  (s/split-lines (str (s/join " " label)
                            ":" "\n" uri))
        ;; (map #(str "i" %))
        (map #(str %))
        (s/join "\n")
        (str "\n")))
