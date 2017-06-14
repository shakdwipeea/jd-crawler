(ns jd-crawler.comedk
  (:import java.lang.Integer))

(require '[clojure-csv.core :as csv])

(defn numeric? [s]
  (if-let [s (seq s)]
    (let [s (if (= (first s) \-) (next s) s)
          s (drop-while #(Character/isDigit %) s)
          s (if (= (first s) \.) (next s) s)
          s (drop-while #(Character/isDigit %) s)]
      (empty? s))))

(when (numeric? "")
  1)

(conj '(1 2) 3)

(if "" :ok :notok)

(let [my-rank 10000
      my-branch 18]
  (->> "resources/comedk.csv"
       slurp
       csv/parse-csv
       (reduce (fn [acc e]
                 (if-not (empty? (get e 0))
                   (conj acc {:name (get e 0)
                              :category (get e 1)
                              :cutoff-rank (get e my-branch)})
                   (conj acc {:name (:name (peek acc))
                              :category (get e 1)
                              :cutoff-rank (get e my-branch)}))) '())
       (filter (fn [{:keys [name cutoff-rank]}]
                 (when (numeric? cutoff-rank)
                   (< my-rank (Long/parseLong cutoff-rank)))))
       (map (fn [{:keys [name cutoff-rank category]}]
              {:name name
               :category (case category
                           "GM" :general
                           "HKR" :karnataka
                           :other)
               :cutoff-rank (Long/parseLong cutoff-rank)}))
       (filter #(= :general (:category %)))
       (sort-by :cutoff-rank)))



(get [1 2 3 4 5] 3)















