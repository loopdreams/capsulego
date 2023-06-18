(ns single.linewrap
  (:require [clojure.string :as s]))

(def line-length 70)

(defn index-of-spaces [line]
  (let [m (re-matcher #" " line)]
    ((fn step []
       (when (. m find)
         (cons (. m start)
                (lazy-seq (step))))))))

(defn split-lines-at-limit [line]
  (if (> (count line) line-length)
    (let [split-point (->> line
                           (index-of-spaces)
                           (sort-by #(Math/abs (- line-length %)))
                           (first))]
      (str (str (subs line 0 split-point) "\n")
           (split-lines-at-limit (subs line (inc split-point)))))
    line))

(defn wrap-line [line]
  (s/replace
   (split-lines-at-limit line)
   #"\n\s+" "\n"))
