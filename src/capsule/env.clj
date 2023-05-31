(ns capsule.env)

;; TODO way of setting this as option, or defualt to name of gemlog directory
;; (def domain "gopher://spool-five.com")
;; (def gemlog-directory "/home/eoin/test-gemlog")
;; (def marked-indexes ["/home/eoin/test-gemlog/archive.gmi"])

;; (defn destination-directory
;;   ([] (str gemlog-directory "_gopherhole"))
;;   ([name] (fs/absolutize name)))
;;
;; (def destination-directory (str gemlog-directory "_gopherhole"))

(def domain (atom nil))
(def gemlog-directory (atom nil))
(def destination-directory (atom nil))
(def marked-indexes (atom []))
(def map-of-files (atom {}))
