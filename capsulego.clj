#!/usr/bin/env bb

(require '[babashka.cli :as cli])
(require '[capsule.main :as run])
(require '[babashka.fs :as fs])

(def spec {:gemlog {:ref "<directory>"
                    :desc "The directory you wish to covert to a gopherhole"
                    :alias :g}
           :to      {:ref "<directory>"
                     :desc "The destination directory for the conversion"
                     :alias :o
                     :default-desc "gemlog_gopherhole"}
           :domain {:desc "Domain name for serving the new gopherhole"
                    :alias :d
                    :default-desc "localhost"
                    :default "localhost"}})

(defn print-opts []
  (println (cli/format-opts {:spec spec :order [:gemlog :to :domain]})))


(defn test-fn [arg]
  (if (fs/directory? arg) (println "yes") (println "no")))


(let [[gemlog destination domain] *command-line-args*]
  (test-fn gemlog)
  (when (empty? gemlog)
    (println "Usage: <gemlog-directory> <destination-directory (defaults to gemlog_gopherhole)> <domain (defaults to localhost)>")
    (System/exit 1))
  (if (fs/directory? gemlog)
    (run/main-convert :input-dir gemlog
                      :output-dir (when destination destination)
                      :domain (if domain domain "localhost"))
    (println "Not a valid directory")))
