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
  (let [index-files (map first (:index (:map-of-files @env/vars)))
        matcher (re-pattern uri)]
    (if (every? nil? (map #(re-find matcher %) index-files))
      :text-file
      :directory)))

(defn make-gm-relative-link [link label]
  (let [link (s/replace link #".gmi" "")]
    (str "0" label "\t" link "\t" (:domain @env/vars) "\t" 70)))

(defn gmap-link [line file]
  (let [[_ uri & label] (s/split line #" ")]
    (if (l/internal-link? uri)
      (make-gm-relative-link uri (s/join " " label))
      (gmap/gm-link-format uri label))))

