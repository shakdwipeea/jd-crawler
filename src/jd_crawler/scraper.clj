(ns jd-crawler.scraper
  (:require [clojure.java.io :refer [as-url]])
  (:import org.apache.commons.validator.routines.UrlValidator))

(require '[skyscraper :as scraper])
(require '[net.cgrand.enlive-html :as enlive])
(require '[cheshire.core :as cheshire])


(defn my-error-handler [url error]
  (println "error handler called")
  [])


;; Root Page

(defn- process-home [elements]
  (->> elements
       (map (fn [e]
              {:url (scraper/href e)
               :text (-> e
                         (enlive/select [:span.hotkeys-text])
                         first
                         enlive/text)
               :processor :options-page}))
       (filter (fn [{:keys [url] :as e}]
                 (println e)
                 (assoc e :url (re-find #"Mumbai" url))))
       (map (fn [{:keys [url] :as e}]
              (assoc e
                     :url
                     (clojure.string/replace url #"Mumbai" "Muzaffarpur"))))))

(scraper/defprocessor root-page
  :cache-template "home"
  :process-fn (fn [res context]
                (println "Processing root")
                (-> res
                    (enlive/select
                     [:ul#sidebarnavleft :li :a])
                    process-home)))


(scraper/scrape :seed :error-handler (fn [url error]
                                       [{:context "Error occured at url %s and the status is %s"
                                         :error-url url
                                         :status (:status error)}]))

;; Options page
(defn valid-url? [url-str]
  (let [validator (UrlValidator.)]
    (if (.isValid validator url-str)
      url-str
      nil)))


(defn process-options [opt-nodes]
  (map (fn [n]
         {:url (valid-url? (scraper/href n)
                )
          :text (-> n
                    (enlive/select [:span.meditle])
                    first
                    enlive/text)
          :processor :pathfinder}) opt-nodes))

(scraper/defprocessor options-page
  :cache-template "options/:text"
  :process-fn (fn [res context]
                (println "Processing options page " (:text context))
                (-> res
                    (enlive/select [:div#mnintrnlbnr :li :a])
                    process-options)))


;; pathfinder

;; The sole existence of this processor is that jd divides the top listing as 
;; either top>options>results>listing or top>options>options>results>listing. 
;; So we have set out to find its course. May the force be with us.

(scraper/defprocessor pathfinder
  :cache-template "path/:text"
  :process-fn (fn [res context]
                (println "finding path for " context)
                (if (empty? (enlive/select res [:div#mnintrnlbnr :li :a]))
                  (assoc context :processor :options-page)
                  (merge context {:base-link (:url context)
                                  :page "1"
                                  :processor :results-page}))))


;; Results page

(defn append-to-link [link page]
  (if (= (last link) \/)
    (str link "page-" page)
    (str link "/" "page-" page)))

(defn process-result-item [item]
  {:name (enlive/text item)
   :url (scraper/href item)
   :processor :listing-page})

(defn process-results [results {:keys [base-link page]}]
  (let [context-list (map #(process-result-item %) results) ]
    (when-not (empty? context-list)
      (conj context-list {:page (str (inc (read-string page)))
                          :url (append-to-link base-link (inc (read-string page)))
                          :processor :results-page}))))

(scraper/defprocessor results-page
  :cache-template "result/:text/:page"
  :process-fn (fn [res {:keys [page text] :as context}]
                (println "processing page " page " of " text)
                (process-results
                 (enlive/select res
                                [:section.rslwrp :span.jcn :a])
                 context)))


;; Business Listing page 


(scraper/defprocessor listing-page
  :cache-template "listing/:name"
  :process-fn (fn [res context]
                (println "getting listing of " (:name context))
                {:phones (map enlive/text (enlive/select
                                           res
                                           [:ul#comp-contact :div.telCntct :a]))
                 :address (enlive/text (first (enlive/select
                                               res
                                               [:ul#comp-contact :span#fulladdress :span])))
                 :name (enlive/text (first (enlive/select
                                            res
                                            [:div.company-details :span.fn])))}))

;; options url "https://www.justdial.com/Muzaffarpur/279/Anything-on-Hire_fil"
;; res-url "https://www.justdial.com/Muzaffarpur/Event-Organisers/nct-10194150"

;; context for results-page
;; {:url "https://www.justdial.com/Muzaffarpur/Car-Hire/nct-10076456"
;;     :processor :listing-page
;;     :base-link "https://www.justdial.com/Muzaffarpur/Car-Hire/nct-10076456"
;;     :page "1"}


(defn seed [& _]
  [{:url "https://www.justdial.com"
    :processor :root-page}])


(let [results (scraper/scrape (seed)
                              :processed-cache false
                              :http-cache true
                              :error-handler :my-error-handler
                              :retries 1)]
  (spit "jd-data.json" (cheshire/generate-string results)))
