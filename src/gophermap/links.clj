(ns gophermap.links
  (:require [babashka.fs :as fs]
            [clojure.string :as s]
            [gophermap.lines :as gmap]
            [single.rellinks :as rel]
            [single.lines :as l]
            [capsule.env :as env]))


(defn translate-uri [uri]
  (if (= (fs/extension uri) "gmi")
    (str (fs/strip-ext uri))
         ;; ".txt")
    uri))

(defn set-link-type [uri]
  (let [index-files (map first (:index @env/map-of-files))
        matcher (re-pattern uri)]
    (if (every? nil? (map #(re-find matcher %) index-files))
      :text-file
      :directory)))

;;
;; (defn make-relative-link [link label]
;;   (let [domain @env/domain
;;         uri (s/replace link domain "")
;;         type (set-link-type uri)]
;;     (str (if (= type :directory) "1" "0")
;;          (if label (s/join label) uri)
;;          "\t" uri "\t" domain "\t" "70")))

(defn make-gm-relative-link [link label]
  (let [protocol "gopher://"
        domain @env/domain
        matcher (str protocol domain "/")]
    (str "0"
         label
         "\t"
         (s/replace link matcher ""))))


        

;; TODO

;; (defn gm-link [line file]
;;   (let [[_ uri & label] (s/split line #" ")
;;         parents (gemlog->phlog (s/join "/" (butlast (rel/parent-directories file))))]
;;     (if (internal-link? uri)
;;       (make-relative-link uri label parents)
;;       (gmap/gm-link-format uri label))))


;;  TODO use the rellinks function to return link and then just restructure it...
(defn gmap-link [line file]
  (let [[_ uri & label] (s/split line #" ")]
    (if (l/internal-link? uri)
      (make-gm-relative-link (rel/convert-relative-links uri file) (s/join " " label))
      (gmap/gm-link-format uri label))))

(make-gm-relative-link

 "gopher://localhost/about.txt" "About")
