(ns capsule.main
  (:require [single.convert :as convert]
            [capsule.env :as env]
            [capsule.map :as map]))

(defn update-env-atoms [input-dir domain output-dir indexes]
  (reset! env/domain domain)
  (reset! env/gemlog-directory input-dir)
  (reset! env/destination-directory output-dir)
  (reset! env/marked-indexes indexes))

;; (defn convert-all-indexes []
;;   (let [origs (map first (:index @env/map-of-files))
;;         to    (map second (:index @env/map-of-files))]
;;     (map #(convert/convert-index-file %1 %2) origs to)))

(defn indexgmi->gophermap []
  (let [[orig to] (:gophermap @env/map-of-files)]
    (convert/convert-index-file orig to)))

(defn convert-all-gemtext []
  (let [origs (map first (:gemtext @env/map-of-files))
        to    (map second (:gemtext @env/map-of-files))]
    (map #(convert/convert-file %1 %2 :no-headers) origs to)))


(defn main-convert [& {:keys [input-dir domain output-dir indexes]
                       :or {output-dir (str input-dir "_gopherhole")
                            domain "localhost"
                            indexes ""}}]
  (if input-dir
    (do
      (update-env-atoms input-dir domain output-dir indexes)
      (map/map-files @env/gemlog-directory
                     @env/destination-directory)
      ;; @env/marked-indexes)
      ;; (println "Converting Indexes...")
      ;; (convert-all-indexes)
      (println "Converting Gemtext...")
      (convert-all-gemtext)
      (indexgmi->gophermap))
    (println "No Input Directory Provided!")))


(comment
  (main-convert :input-dir "/home/eoin/code/clojure/capsulego/test-gemlog"
                :output-dir "/var/gopher"))
