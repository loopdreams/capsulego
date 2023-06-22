#!/usr/bin/env bb

(require '[babashka.cli :as cli])
(require '[babashka.fs :as fs])
(require '[app.convert :refer [convert-file capsule->gopherhole]])

(def capsule-spec {:capsule {:ref   "<directory>"
                             :desc  "The directory you wish to covert to a gopherhole"
                             :alias :c}
                   :output  {:ref          "<directory>"
                             :desc         "The destination directory for the conversion"
                             :alias        :o
                             :default-desc "gemlog_gopherhole"}
                   :domain  {:ref          "<domain>"
                             :desc         "Domain name for serving the new gopherhole"
                             :alias        :d
                             :default-desc "localhost"
                             :default      "localhost"}})

(def single-spec {:file   {:ref   "<file>"
                           :desc  "The gemtext file you wish to convert"
                           :alias :f}
                  :output {:ref         "<destination>"
                           :alias       :o
                           :defaul-desc "file_gopher.txt"}
                  :domain {:ref          "<domain>"
                           :alias        :d
                           :desc         "Domain name (for converting relative links)"
                           :default-desc "localhost"
                           :default      "localhost"}})

(defn help
  [_]
  (println
   (str "Usage:\n"
    "capsulego capsule \n"
    (cli/format-opts {:spec capsule-spec :order [:capsule :output :domain]})
    "\n"
    "capsulego file \n"
    (cli/format-opts {:spec single-spec :order [:file :output :domain]}))))


;; TODO currently 'gemlog' is set to current working directory, would be better to just have this as optional in single covert
(defn convert-single
  [{:keys [opts]}]
  (if (and (:file opts) (fs/exists? (:file opts)))
      (convert-file
       (:file opts)
       (or (:output opts) (str (fs/strip-ext (fs/absolutize (:file opts))) "_gopher.txt"))
       (str (last (fs/components (fs/cwd))))
       (or (:domain opts) "localhost"))
      (help opts)))


(defn convert-capsule
  [{:keys [opts]}]
  (cond
    (or (nil? (:capsule opts))
        (not (fs/exists? (:capsule opts)))) (do (println "No capsule directory provided...") (help opts))
    (and (not (nil? (:output opts)))
         (false? (:overwrite opts))
         (fs/exists? (:output opts)))       (do (println "Output directory already exists, aborting...") (System/exit 1))
    :else
    (capsule->gopherhole
     (:capsule opts)
     (or (:output opts) (str (fs/absolutize (:capsule opts)) "_gopher"))
     (or (:domain opts) "localhost"))))



(def table
  [{:cmds ["capsule"] :fn convert-capsule :spec capsule-spec}
   {:cmds ["file"] :fn convert-single :spec single-spec}
   {:cmds [] :fn help}])


(cli/dispatch table *command-line-args*)




