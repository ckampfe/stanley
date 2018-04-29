(ns stanley.rss
  (:require [clj-rss.core :as rs])
  (:import [java.time LocalDate]
           [java.lang Integer])
  (:gen-class))

(defn str->Date [date-string]
  (let [date-vec (map #(new java.lang.Integer ^java.lang.String %)
                      (clojure.string/split date-string #"-"))
        year (nth date-vec 0)
        month (nth date-vec 1)
        day (nth date-vec 2)]

    (java.sql.Date/valueOf ^java.time.LocalDate (LocalDate/of
                                                  ^java.lang.Integer year
                                                  ^java.lang.Integer month
                                                  ^java.lang.Integer day))))

(defn feed [post-titles
            html-post-names
            post-created-ats
            post-formatted-contents]

  (let [sorted-by-date-descending
        (->> (interleave post-titles
                         html-post-names
                         post-created-ats
                         post-formatted-contents)
             (partition 4)
             (sort-by (fn [[_ _ created-at _]] created-at))
             reverse)]

    (apply rs/channel-xml
           (assoc {}
                  :title "Clark Kampfe"
                  :link "https://zeroclarkthirty.com"
                  :description "Clark Kampfe - zeroclarkthirty.com")
           (pmap (fn [[title post-name created-at formatted-contents]]
                   (assoc {}
                          :title title
                          :link (str "https://zeroclarkthirty.com/" post-name)
                          :pubDate (str->Date created-at)
                          :description formatted-contents))
                 sorted-by-date-descending))))
