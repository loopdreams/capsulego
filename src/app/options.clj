(ns app.options
  (:require [clojure.edn :as edn]
            [babashka.fs :as fs]))

(defn validate-opts [opts]
  (let [error-msg (fn [opt] (println "Error in config file at option " opt ". Please edit 'config.edn'")
                            (System/exit 1))]
    (cond
      (not (int? (:line-length opts)))          (error-msg :line-length)
      (not (boolean? (:headers opts)))          (error-msg :headers)
      (not (#{"" ".txt"}
            (:file-extension-preference opts))) (error-msg :file-extension-preference)
      (not (boolean? (:overwrite opts)))        (error-msg :overwrite)
      :else                                     opts)))

(def options
  (if (fs/exists? "config.edn") (->> (slurp "config.edn")
                                     edn/read-string
                                     validate-opts)
      {:line-length               70
       :headers                   false
       :file-extension-preference ""
       :overwrite                 false}))
