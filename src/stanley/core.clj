(ns stanley.core
  (:require [stanley.templates :as templates]
            [stanley.rss :as rss]
            [markdown.core :as md :refer [md-to-html-string]])
  (:import [java.io File])
  (:gen-class))

(def ^:dynamic build-dir "build")

(defn files [dir]
  (->> (File. dir)
       (.listFiles)
       (remove #(.isHidden %))
       (remove #(.isDirectory %))
       (map #(.getCanonicalPath %))))

(defn md-files [dir]
  (->> (files dir)
       (filter #(clojure.string/ends-with? % ".md"))))

(defn get-content [post-string]
  (->> post-string
       (#(clojure.string/split % #"\n"))
       (drop 5)
       (clojure.string/join "\n")))

(defn get-frontmatter [post-string]
  (->> post-string
       (#(clojure.string/split % #"\n"))
       (take 5) ;; first 5 lines
       (remove #(clojure.string/starts-with? % "---")) ;; remove `---' lines
       (map #(clojure.string/split % #": "))
       (into {})))

(defn change-ext [filename n replacement]
  (str (clojure.string/join
        (drop-last n filename)) replacement))

(defn to-build-dir [filename] (str build-dir "/" filename))

(defn write! [names contents]
  (dorun (map #(spit (to-build-dir %1) %2)
              names
              contents)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [page-paths              (md-files "./pages")
        page-file-names         (map #(last (clojure.string/split % #"/"))
                                     page-paths)
        html-page-names         (map #(change-ext % 3 ".html") page-file-names)
        pages                   (map slurp page-paths)
        page-contents           (map get-content pages)
        page-frontmatters       (map get-frontmatter pages)
        page-formatted-contents (map md-to-html-string page-contents)
        page-titles             (map #(get % "title") page-frontmatters)
        page-templates          (->> (map templates/page page-titles page-formatted-contents)
                                     (map templates/layout page-titles))

        post-paths              (reverse (md-files "./posts"))
        post-file-names         (map #(last (clojure.string/split % #"/"))
                                     post-paths)
        html-post-names         (map #(change-ext % 3 ".html") post-file-names)
        posts                   (map slurp post-paths)
        post-frontmatters       (map get-frontmatter posts)
        post-contents           (map get-content posts)
        post-formatted-contents (map md-to-html-string post-contents)
        post-titles             (map #(get % "title") post-frontmatters)
        post-created-ats        (map #(get % "created") post-frontmatters)
        post-templates          (->> (map templates/post post-titles
                                          post-created-ats
                                          post-formatted-contents)
                                     (map templates/layout post-titles))

        index-template (templates/layout "Clark Kampfe"
                                         (templates/index html-post-names
                                                          post-titles
                                                          post-created-ats))

        rss-feed                (rss/feed post-titles
                                          html-post-names
                                          post-created-ats
                                          post-formatted-contents)]

    (write! ["main.css"] [templates/stylesheet])
    (write! html-page-names page-templates)
    (write! html-post-names post-templates)
    (write! ["index.html"] [index-template])
    (write! ["feed"] [rss-feed])))
