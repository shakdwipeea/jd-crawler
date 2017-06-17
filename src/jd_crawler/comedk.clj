(ns jd-crawler.comedk
  (:import java.lang.Integer))

(require '[clojure-csv.core :as csv])

(require '[cats.core :as m])
(require '[cats.builtin])
(require '[cats.monad.maybe :as maybe])

(defn numeric? [s]
  (if-let [s (seq s)]
    (let [s (if (= (first s) \-) (next s) s)
          s (drop-while #(Character/isDigit %) s)
          s (if (= (first s) \.) (next s) s)
          s (drop-while #(Character/isDigit %) s)]
      (empty? s))))

(when (numeric? "")
  1)

(get (conj '(1 2) 3))

(if "" :ok :notok)

(+ nil nil)



(defn branch-rank-extractor [data branch]
  {:cutoff-rank (get data branch)})


(/ 24 8)

(maybe/just (get '["CTM CONSTRUCTION TECHNOLOGY AND MANAGEMENT"] 11))

(defn avrg-rank-extractor [data]
(println data)
  (let [filtered (map #(Long/parseLong %) (filter nil? [(get data 11)
                                                      (get data 15)
                                                      (get data 18)]))]
    (println filtered)
    {:avrg-rank (/ (reduce + 0 filtered) 3)}))

(filter (and (complement nil?) integer?) [12 "as"])


(defn extract-data [extractor data]
  (reduce (fn [acc e]
                 (when-not (empty? (get e 0))
                   (conj acc (merge {:name (get e 0)
                                     :category (get e 1)}
                                    (extractor e)))
                   ;; (conj acc (merge  {:name (:name (peek acc))
                   ;;                    :category (get e 1)}
                   ;;                   (extractor data)))
                   ))
          '()
          data))

(defn get-eligible-colleges [data rank]
  (filter (fn [{:keys [name cutoff-rank]}]
                 (when (numeric? cutoff-rank)
                   (< rank (Long/parseLong cutoff-rank)))) data))


(defn add-category [data]
  (map (fn [{:keys [name cutoff-rank category]}]
              {:name name
               :category (case category
                           "GM" :general
                           "HKR" :karnataka
                           :other)
               :cutoff-rank (Long/parseLong cutoff-rank)}) data))

(let [my-rank 10000
      my-branch 18]
  (->> "resources/comedk.csv"
       slurp
       csv/parse-csv
       (extract-data avrg-rank-extractor)
       ;; (get-eligible-colleges my-rank)
       ;; add-category
       (filter #(not= 0 (:avrg-rank %)))
       (sort-by :avrg-rank)))
