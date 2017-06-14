(ns jd-crawler.core
  (:gen-class))

(import '(org.openqa.selenium.chrome ChromeOptions
                                     ChromeDriverService
                                     ChromeDriverService$Builder
                                     ChromeDriver)
        '(org.openqa.selenium.remote RemoteWebDriver
                                     DesiredCapabilities)
        '(org.openqa.selenium.interactions Actions)
        '(org.openqa.selenium By
                              Keys)
        '(java.io File))

(require '[skyscraper :as scraper])
(require '[net.cgrand.enlive-html :as enlive])

(defn open-page [url]
  (let [chromedriver (ChromeDriver. (doto (ChromeOptions.)
                                      (.setBinary
                                       (File. "/usr/bin/google-chrome-beta"))))]
    (.get chromedriver url)
    chromedriver))

(defn byid [id page]
  (.findElement page (By/id id)))

(defn bycss [css-sel element]
  (.findElementsByCssSelector element css-sel))

(defn parse-page [page list-container-id link-sel attr]
  (->> page
       (byid list-container-id)
       (bycss link-sel)
       (map #(.getAttribute % attr))
       (remove nil?)))

(defn extract-list-attr
  [url list-container-id link-sel attr]
  (parse-page (open-page url)
              list-container-id
              link-sel
              attr))


(defn extract-list-links [url list-container-id link-sel]
  (extract-list-attr url list-container-id "href"))


(defn- patch-city [urls]
  (->> (extract-list-links "https://www.justdial.com"
                           "sidebarnavleft"
                           "li>a")
       (filter #(re-find #"Bangalore" %))
       (map #(clojure.string/replace % #"Bangalore" "Muzaffarpur"))))

(defn parse-type-links []
  (extract-list-links
   "https://www.justdial.com/Muzaffarpur/279/Anything-on-Hire_fil"
   "mnintrnlbnr"
   "li>a"))

(def det-page-url "https://www.justdial.com/Muzaffarpur/Event-Organisers/nct-10194150")

;; (.executeScript  page
;;                    "window.scrollTo(0,document.body.scrollHeight);"
;;                    (make-array Object 0))

(last "Akash")

(let [page (open-page det-page-url)]
  ;; (SelectWindow. (Windows. page))
  (.switchTo page)
  (count (parse-page page "tab-5" "ul>li" "data-href")))


;; (count (extract-list-attr det-page-url "tab-5" "ul>li" "data-href"))

(defn append-to-link [link page]
  (if (= (last link) \/)
    (str link "page-" page)
    (str link "/" "page-" page)))

;; (defn get-link-attrs [page]
;;   (extract-list-attr (append-to-link base-link page)
;;                      "tab-5"
;;                      "ul>li"
;;                      "data-href"))

(defn collect-links [base-link]
  (loop  [cur-page 0
          link-coll '()]
    (let [link-attr (extract-list-attr
                        (append-to-link base-link cur-page)
                        "tab-5"
                        "ul>li"
                        "data-href")]
      (if (empty? link-attr)
        link-coll
        (recur (inc cur-page)
               (conj link-coll link-attr))))))

(collect-links det-page-url)

;; (-> service-url
;;     (RemoteWebDriver. (DesiredCapabilities/chrome))
;;     .get "https://www.justdial.com")

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "hello"))







