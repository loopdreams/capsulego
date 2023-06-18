(ns capsule.map
  (:require [babashka.fs :as fs]
            [clojure.string :as s]
            [capsule.env :as env]))

(defn get-all-files [gemlog-dir]
  (map str (fs/glob gemlog-dir "**")))

(defn non-gmi-files [all-files]
  (into [] (remove #(re-find #".gmi" %) all-files)))

(defn index-gophermap [all-files]
  (let [matcher (re-pattern (str (:gemlog-directory @env/vars) "/index.gmi"))
        gophermap (filter #(re-find matcher %) all-files)]
    (if (> (count gophermap) 1)
      (println "More than one index.gmi found in root, skipping gophermap creation...")
      gophermap)))

(defn gmi-files [all-files gophermap]
  (remove #{gophermap}
          (filter #(re-find #".gmi" %) all-files)))

(defn construct-parent-dirs
  "also changes 'gemlog' folder to 'phlog' folder in destination directory."
  [parent gemlog-directory]
  (let [parent-dir (.toString parent)]
    ;; (s/replace
    (s/replace parent-dir gemlog-directory "")))
     ;; "gemlog" "phlog"))

;; TODO This fails if there is a trailing slash in gemlog-directory name provided...
(defn convert-path
  [gmi-file gemlog-directory destination-directory]
  (str destination-directory
       (construct-parent-dirs (fs/parent gmi-file) gemlog-directory)
       "/"
       (fs/strip-ext (fs/file-name gmi-file))))


;; TODO Have user option to set gophermap file format (just 'gophermap' or extension '.gophermap', this will depend on how servers read this
;; (defn convert-index-path
;;   [index-file gemlog-directory destination-directory]
;;   (str destination-directory
;;        (construct-parent-dirs (fs/parent index-file) gemlog-directory)
;;        "/"
;;        (fs/strip-ext (fs/file-name index-file))
;;        ".gophermap"))



(defn gemtxt-files-to-convert [orig-files gemlog-directory destination-directory]
  (let [convert-to (map #(convert-path % gemlog-directory destination-directory)
                        orig-files)]
    (into [] (partition-all 2 (interleave orig-files convert-to)))))

(defn index-to-convert [gophermap-file gemlog-directory destination-directory]
  (let [convert-to (s/replace (convert-path gophermap-file
                                            gemlog-directory
                                            destination-directory)
                              #"index"
                              "gophermap")]
                              
    [gophermap-file convert-to]))

(defn map-files [gemlog-directory destination-directory]
  (let [all-files (get-all-files gemlog-directory)
        gophermap (first (index-gophermap all-files))
        gmi-files (gmi-files all-files gophermap)]
    (swap! env/vars assoc :map-of-files {:gemtext   (gemtxt-files-to-convert
                                                     gmi-files
                                                     gemlog-directory
                                                     destination-directory)
                                         :gophermap (index-to-convert
                                                     gophermap
                                                     gemlog-directory
                                                     destination-directory)
                                         :regular   (non-gmi-files all-files)})))
