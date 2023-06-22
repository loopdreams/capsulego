(ns app.main
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [babashka.fs :as fs]
            [app.convert :refer [convert-file]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Categorize and map files

;; (defn file-parent-dirs
;;   "The path to file (from capsule root)"
;;   [file capsule-dir]
;;   (str/replace (str (fs/parent file)) capsule-dir ""))

;; (defn convert-path
;;   "returns new path for file, which will be writted to after conversion."
;;   [file capsule-directory destination-directory]
;;   (let [ext (:file-extension-preference options)]
;;     (str
;;      (str (fs/file destination-directory)) ;; the fs/file cooercion here removes trailing slashes
;;      (file-parent-dirs file (str (fs/file capsule-directory)))
;;      "/"
;;      (if (re-find #".gmi" file)
;;        (str (fs/strip-ext (fs/file-name file)) ext)
;;        (fs/file-name file)))))

;; (defn sort-files [capsule-dir]
;;   (let [all-files (map str (fs/glob capsule-dir "**"))]
;;     (reduce (fn [[regular-list gemtext-list] file]
;;               (cond
;;                 (fs/directory? file) [regular-list gemtext-list]
;;                 (re-find #".gmi" file)[regular-list (conj gemtext-list file)]
;;                 :else [(conj regular-list file) gemtext-list]))
;;             [[] []]
;;             all-files)))

;; (defn prepare-files [capsule-dir destination-dir]
;;   (let [[regular gemtext] (sort-files capsule-dir)
;;         reg-to            (map #(convert-path % capsule-dir destination-dir) regular)
;;         gemtext-to        (map #(convert-path % capsule-dir destination-dir) gemtext)]
;;     {:regular-files regular
;;      :regular-to    reg-to
;;      :gemtext       gemtext
;;      :gemtext-to    gemtext-to}))


;; ;; (defn index-gophermap [all-files]
;; ;;   (let [matcher (re-pattern (str (:gemlog-directory @env/vars) "/index.gmi"))
;; ;;         gophermap (filter #(re-find matcher %) all-files)]
;; ;;     (if (> (count gophermap) 1)
;; ;;       (println "More than one index.gmi found in root, skipping gophermap creation...")
;; ;;       gophermap)))


;; ;; TODO Have user option to set gophermap file format (just 'gophermap' or extension '.gophermap', this will depend on how servers read this
;; ;; (defn convert-index-path
;; ;;   [index-file gemlog-directory destination-directory]
;; ;;   (str destination-directory
;; ;;        (construct-parent-dirs (fs/parent index-file) gemlog-directory)
;; ;;        "/"
;; ;;        (fs/strip-ext (fs/file-name index-file))
;; ;;        ".gophermap"))


;; (defn capsule->gopherhole [capsule-dir output-dir domain]
;;   (if (fs/exists? domain) (println "Destination directory already exists...Aborting")
;;       (let [{:keys [regular-files regular-to gemtext gemtext-to]}
;;             (prepare-files capsule-dir output-dir)]
;;         (dorun (map #(convert-file %1 %2 capsule-dir domain) gemtext gemtext-to))
;;         (dorun (map #(fs/copy %1 %2) regular-files regular-to)))))
