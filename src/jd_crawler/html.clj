(ns jd-crawler.html
  (:require [net.cgrand.enlive-html :as enlive]))


(defn select-first [e sel]
  (-> e (enlive/select sel) first enlive/text))

(defn select* [res & sels]
  (map #(enlive/select res %) sels))
