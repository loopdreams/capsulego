(ns capsule.env)

(def vars (atom {:domain nil
                 :gemlog-directory nil
                 :destination-directory nil
                 :marked-indexes nil
                 :map-of-files nil}))

;; (def domain (atom nil))
;; (def gemlog-directory (atom nil))
;; (def destination-directory (atom nil))
;; (def marked-indexes (atom []))
;; (def map-of-files (atom {}))
