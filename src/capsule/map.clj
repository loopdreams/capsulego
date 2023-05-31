(ns capsule.map
  (:require [babashka.fs :as fs]
            [clojure.string :as s]
            [capsule.env :as env]))

(defn get-all-files [gemlog-dir]
  (map str (fs/glob gemlog-dir "**")))

;; (defn non-gmi-files [all-files]
;;   (let [non-gmi-files (remove #(re-find #".gmi" %) all-files)]
;;     (into [] non-gmi-files)))

(defn non-gmi-files [all-files]
  (into [] (remove #(re-find #".gmi" %) all-files)))

(defn gmi-files [all-files]
  (filter #(re-find #".gmi" %) all-files))

;; (defn index-files [gmi-files additional-index-files]
;;   (let [index-files (filter #(re-find #"/index.gmi$" %) gmi-files)]
;;     (if (empty? additional-index-files) index-files
;;         (first
;;          (map #(conj index-files %)
;;               additional-index-files)))))

;; (defn filter-index-files
;;   [gmi-files index-files]
;;   (mapcat
;;    (fn [[x n]] (repeat n x))
;;    (apply merge-with - (map frequencies [gmi-files index-files]))))

(defn construct-parent-dirs
  "changing 'gemlog' folder to 'phlog' folder in destination directory."
  [parent gemlog-directory]
  (let [parent-dir (.toString parent)]
    (s/replace
     (s/replace parent-dir gemlog-directory "")
     "gemlog" "phlog")))

;; TODO Altenatively, .txt extension not needed?
;; TODO This fails if there is a trailing slash in gemlog-directory name...
(defn convert-path
  [gmi-file gemlog-directory destination-directory]
  (str destination-directory
       (construct-parent-dirs (fs/parent gmi-file) gemlog-directory)
       "/"
       (fs/strip-ext (fs/file-name gmi-file))))
       ;; ".txt"))


;; TODO Have user option to set gophermap file format (just 'gophermap' or extension '.gophermap', this will depend on how servers read this
(defn convert-index-path
  [index-file gemlog-directory destination-directory]
  (str destination-directory
       (construct-parent-dirs (fs/parent index-file) gemlog-directory)
       "/"
       (fs/strip-ext (fs/file-name index-file))
       ".gophermap"))

(defn gemtxt-files-to-convert [orig-files gemlog-directory destination-directory]
  (let [convert-to (map #(convert-path % gemlog-directory destination-directory)
                        orig-files)]
    (into [] (partition-all 2 (interleave orig-files convert-to)))))

;; (defn index-files-to-convert [orig-files gemlog-directory destination-directory]
;;   (let [convert-to (map #(convert-index-path % gemlog-directory destination-directory)
;;                         orig-files)]
;;     (into [] (partition-all 2 (interleave orig-files convert-to)))))
    
;; (defn sort-gmi-files
;;   [gmi-files marked-indexes]
;;   (let [indexes (index-files gmi-files marked-indexes)
;;         gemtext (filter-index-files gmi-files indexes)]
;;     {:orig-indexes (into [] indexes)
;;      :orig-gemtext (into [] gemtext)}))

;; (defn map-files [gemlog-directory destintation-directory marked-indexes]
;;   (let [all-files (get-all-files gemlog-directory)
;;         gmi-files (gmi-files all-files)
;;         sorted-gmi-files (sort-gmi-files gmi-files marked-indexes)]
;;     (reset! env/map-of-files {:gemtext (gemtxt-files-to-convert
;;                                         (:orig-gemtext sorted-gmi-files)
;;                                         gemlog-directory
;;                                         destintation-directory)
;;                               :index (index-files-to-convert
;;                                       (:orig-indexes sorted-gmi-files)
;;                                       gemlog-directory
;;                                       destintation-directory)
;;                               :regular (non-gmi-files all-files)})))


(defn map-files [gemlog-directory destintation-directory]
  (let [all-files (get-all-files gemlog-directory)
        gmi-files (gmi-files all-files)]
    (reset! env/map-of-files {:gemtext (gemtxt-files-to-convert
                                        gmi-files
                                        gemlog-directory
                                        destintation-directory)
                              :regular (non-gmi-files all-files)})))








