(defproject jd-crawler "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.seleniumhq.selenium/selenium-java "3.4.0"]
                 [skyscraper "0.2.3"]
                 [cheshire "5.7.1"]
                 [korma "0.4.3"]
                 [commons-validator "1.4.1"]
                 [clj-http "3.6.1"]
                 [funcool/cats "2.1.0"]
                 [clojure-csv/clojure-csv "2.0.1"]]
  :main ^:skip-aot jd-crawler.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
