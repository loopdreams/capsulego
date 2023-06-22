(ns app.rellinks
  (:require [babashka.fs    :as fs]
            [clojure.string :as str]
            [app.options :refer [options]]))

(defn parent-directories
  "Returns path components for file as list (excluding root, i.e. gemlog-directory)
  gemlog-directory is the full path to the gemlog-directory"
  [file-path gemlog-directory]
  (mapv str (fs/components (str/replace file-path gemlog-directory ""))))

(defn missing-parents [uri-parents file-parents]
  (let [depth       (- (count file-parents)
                       (count (filter #{".."} uri-parents)))
        replacement (take depth file-parents)
        cleaned     (remove #{".."} uri-parents)]
    (str/join "/" (flatten (conj cleaned replacement)))))

(defn translate-file-extension [uri]
  (str/replace uri #".gmi" (:file-extension-preference options)))

(defn convert-relative-links [uri file gemlog-directory domain]
  (let [parents-uri (parent-directories uri gemlog-directory)
        parents-file (parent-directories file gemlog-directory)
        domain (str domain "/")]
    (if (some #{".."} parents-uri)
      (str "gopher://" domain "0/"
           (translate-file-extension
            (missing-parents parents-uri parents-file)))
      (str "gopher://" domain "0/"
           (translate-file-extension
            (str/join "/" (concat (butlast parents-file) parents-uri)))))))
