(ns stanley.rss
  (:require [clj-rss.core :as rs])
  (:import [java.util GregorianCalendar]))

(defn str->Date [date-string]
  (let [date-vec (map #(Integer. %) (clojure.string/split date-string #"-"))
        year (nth date-vec 0)
        month (nth date-vec 1)
        day (nth date-vec 2)]

    (->> (GregorianCalendar. year month day)
         (.getTime))))

(defn feed [post-titles
                 html-post-names
                 post-created-ats
                 post-formatted-contents]

    (apply rs/channel-xml
           (assoc {}
                  :title "Clark Kampfe"
                  :link "https://zeroclarkthirty.com"
                  :description "Clark Kampfe - zeroclarkthirty.com")
           (map #(assoc {}
                        :title %1
                        :link (str "https://zeroclarkthirty.com/" %2)
                        :pubDate (str->Date %3 )
                        :description %4)
                post-titles
                html-post-names
                post-created-ats
                post-formatted-contents)))
