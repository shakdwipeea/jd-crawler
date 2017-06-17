(ns jd-crawler.persist
  (:require [clojure-csv.core :as csv]))


(defn save-listing [listing]
  (spit "jd-data.csv"
        (-> listing vals vector csv/write-csv)
        :append true))


(defn clear-listing []
  (spit "jd-data.csv" ""))

(count  (csv/parse-csv (slurp "jd-data.csv")))

(spit "mfp-data.csv" (csv/write-csv (set )))
