(ns capsule.main
  (:require [single.convert :as convert]
            [capsule.env :as env]
            [capsule.map :as map]))

(defn update-env-atoms [input-dir domain output-dir indexes]
  (swap! env/vars assoc
          :domain domain
          :gemlog-directory input-dir
          :destination-directory output-dir
          :marked-indexes indexes))


(defn indexgmi->gophermap []
  (let [[orig to] (:gophermap (:map-of-files @env/vars))]
    (convert/convert-index-file orig to)))

(defn convert-all-gemtext []
  (let [origs (map first (:gemtext (:map-of-files @env/vars)))
        to    (map second (:gemtext (:map-of-files @env/vars)))]
    (map #(convert/convert-file %1 %2 :no-headers) origs to)))


(defn main-convert [& {:keys [input-dir domain output-dir indexes]
                       :or   {output-dir (str input-dir "_gopherhole")
                              domain     "localhost"
                              indexes    ""}}]
  (if input-dir
    (dosync
     (update-env-atoms input-dir domain output-dir indexes)
     (map/map-files (:gemlog-directory @env/vars)
                    (:destination-directory @env/vars))
     (println "Converting Gemtext...")
     (convert-all-gemtext))
     ;; (indexgmi->gophermap)
           
    (println "No Input Directory Provided!")))


(comment
  (main-convert :input-dir "/home/eoin/gemini-test"
                :output-dir "/var/gopher"))
