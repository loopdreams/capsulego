(ns single.rellinks
  (:require [babashka.fs    :as fs]
            [capsule.env    :as env]
            [clojure.string :as s]))

(defn gemlog->phlog [path]
  (s/replace path #"gemlog" "phlog"))

(defn parent-directories [path]
  (let [re-path (s/replace path (re-pattern (:gemlog-directory @env/vars)) "")]
    (map str (fs/components re-path))))

(defn missing-parents [uri-parents file-parents]
  (let [depth       (- (count file-parents)
                       (count (filter #{".."} uri-parents)))
        replacement (take depth file-parents)
        cleaned     (remove #{".."} uri-parents)]
    (s/join "/" (flatten (conj cleaned replacement)))))

(defn translate-file-extension [uri]
  (let [find (fn [type]
               (remove nil?
                       (map #(re-find (re-pattern (str "/" uri)) %)
                            (map first (type (:map-of-files @env/vars))))))]
    (cond
      (seq (find :index))   (s/replace uri #".gmi" ".gophermap")
      ;; (seq (find :gemtext)) (s/replace uri #".gmi" ".txt")
      (seq (find :gemtext)) (s/replace uri #".gmi" "")
      :else                 uri)))




(defn convert-relative-links [uri file]
  (let [parents-uri (parent-directories uri)
        parents-file (parent-directories file)
        domain (str (:domain @env/vars) "/")]
    (if (some #{".."} parents-uri)
      (str "gopher://" domain "0/"
           (translate-file-extension
            (missing-parents parents-uri parents-file)))
      (str "gopher://" domain "0/"
           (translate-file-extension
            (s/join "/" (concat (butlast parents-file) parents-uri)))))))
