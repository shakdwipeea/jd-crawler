(ns jd-crawler.scraper
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [jd-crawler.html :refer [select* select-first]]
            [jd-crawler.persist :as persist]
            [net.cgrand.enlive-html :refer [attr?]])
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

;; Options page
(defn valid-url? [url-str]
  (let [validator (UrlValidator.)]
    (if (.isValid validator url-str)
      url-str
      nil)))

(defn emptyseq? [seq]
  (if (empty? seq)
    false
    seq))

;; so many or for desparately finding a text
(defn process-options [opt-nodes]
  (map (fn [n]
         {:url (valid-url? (scraper/href n))
          :text (or (emptyseq? (select-first n [:span.meditle]))
                    (emptyseq? (select-first n [:span.meditle1]))
                    (scraper/href n))
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
  :cache-template "path/:text/:url"
  :process-fn (fn [res context]
                (println "finding path for " context)
                (if (empty? (enlive/select res [:div#mnintrnlbnr :li :a]))
                  (do (println "the path was results")
                      (merge context {:base-link (:url context)
                                      :page "1"
                                      :text (:text context)
                                      :processor :results-page}))
                  (do (println "the path was options")
                      (assoc context :processor :options-page)))))


;; Results page

(defn append-to-link [link page]
  (if (= (last link) \/)
    (str link "page-" page)
    (str link "/" "page-" page)))

(defn process-result-item [item]
  {:name (enlive/text item)
   :url (scraper/href item)
   :processor :listing-page})



(empty? '())

(< 3 1)


(defn ok [g ts hs]
  [g ts hs])

(apply ok 12 '(25 16))

(defn process-results [{:keys [base-link page text]} listings next]
  (let [context-list (map #(process-result-item %) listings)]
    (println "sorry bu results are " (count listings))
    (when-not (< (count context-list) 3)
      (conj context-list {:text text
                          :url (scraper/href next)
                          :processor :results-page}))))

(scraper/defprocessor results-page
  :cache-template "result/:text/:page"
  :process-fn (fn [res {:keys [page text] :as context}]
                (println "processing page " page " of " text)
                (apply process-results context (select* res
                                                        [:section.rslwrp :span.jcn :a]
                                                        [:div#srchpagination [:a (attr? :rel)] ]))))

;; Business Listing page 

(scraper/defprocessor listing-page
  :cache-template "listing/:name"
  :process-fn (fn [res context]
                (println "getting listing of " (:name context))
                (let [listing {:phones (str/join "," (map
                                                      enlive/text
                                                      (enlive/select
                                                       res
                                                       [:ul#comp-contact :div.telCntct :a])))
                               :address (-> res
                                            (enlive/select
                                             [:ul#comp-contact
                                              :span#fulladdress
                                              :span])
                                            first
                                            enlive/text)
                               :name (-> res
                                         (enlive/select
                                          [:div.company-details :span.fn])
                                         first
                                         enlive/text)}]
                  (persist/save-listing listing)
                  listing)))




;; options url "https://www.justdial.com/Muzaffarpur/279/Anything-on-Hire_fil"
;; res-url "https://www.justdial.com/Muzaffarpur/Event-Organisers/nct-10194150"

;; context for results-page
;; {:url "https://www.justdial.com/Muzaffarpur/Car-Hire/nct-10076456"
;;     :processor :listing-page
;;     :base-link "https://www.justdial.com/Muzaffarpur/Car-Hire/nct-10076456"
;;     :page "1"}

(println (client/get "https://www.justdial.com/Muzaffarpur/268/12559/Bottle-Feeding-Accessories_b2c/page-2"
                     {:redirect-strategy :none
                      :client-params {"http.protocol.allow-circular-redirects" false
                                      "http.useragent" "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"}}))


(defn seed [& _]
  [{:url "https://www.justdial.com"
    :processor :root-page}])

(persist/clear-listing)

(let [results (scraper/scrape (seed)
                              :processed-cache false
                              :error-handler :my-error-handler
                              :retries 2)]
  (spit "jd-data.json" (cheshire/generate-string results)))
