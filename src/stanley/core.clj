(ns stanley.core
  "
  main module for building the site.
  to run the specs, export the namespace to your repl
  and run:

  (->> (clojure.spec.test/instrument)
       clojure.spec.test/check
       clojure.spec.test/summarize-results
       clojure.pprint/pprint)
  "
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.java.io :as io]
            [instaparse.core :as insta]
            [stanley.templates :as templates]
            [stanley.rss :as rss]
            [markdown.core :as md :refer [md-to-html-string]])
  (:gen-class))

(def ^:dynamic build-dir "build")

(set! *warn-on-reflection* true)

(defn files
  "Given a `dir', list the files,
  removing hidden files and directories, mapping files to
  their canonical path"
  [dir]
  (let [f ^java.io.File (io/file dir)]
    (->> (.listFiles ^java.io.File f)
         (remove #(.isHidden ^java.io.File %))
         (remove #(.isDirectory ^java.io.File %))
         (map #(.getCanonicalPath ^java.io.File %)))))

(defn md-files
  "fetch files in `dir' and select only the ones that
  end in `.md'"
  [dir]
  (->> (files dir)
       (filter #(clojure.string/ends-with? % ".md"))))

(def post-grammar
  "S = Frontmatter PostBody
   Frontmatter = <Dashes> <Newlines> KVS <Newlines> <Dashes> <Newlines>
   Dashes = '---'

   KVS = KV+
   KV = K <':'> <' '> V <'\\n'>

   <K> = #'\\w+'
   <V> = #'(.)+'

   Newlines = '\\n'*

   <PostBody> = #'(?s:.)+'
  ")

(def post-parser (insta/parser post-grammar))

(defn parse-post
  [post-string]
  (let [post-parse-tree (insta/parse post-parser post-string)]
    (if (insta/failure? post-parse-tree)
      (do
        (instaparse.failure/pprint-failure post-parse-tree)
        (throw (RuntimeException. ^String
                (.toString ^instaparse.gll.Failure
                 (insta/get-failure post-parse-tree)))))
      (insta/transform {:KV (fn [k v]
                              (assoc {}
                                     (keyword k)
                                     v))
                        :Frontmatter (fn [kvs]
                                       (apply merge (drop 1 kvs)))
                        :Postbody (fn [postbody] (drop 1 postbody))
                        :S (fn [frontmatter body] {:frontmatter frontmatter :content body})}
                       post-parse-tree))))

(defn change-ext
  "Given a filename, the length of an extension (including the dot),
  and a new extension, return a file with the new extension."
  [filename n replacement]
  (str (clojure.string/join
        (drop-last n filename)) replacement))

(defn to-build-dir [filename] (str build-dir "/" filename))

(defn write!
  "Eagerly write the seq of contents to the seq of names."
  [names contents]
  (dorun (map #(spit (to-build-dir %1) %2)
              names
              contents)))

(defn build-blog []
  (let [page-paths              (md-files "./pages")
        page-file-names         (map #(last (clojure.string/split % #"/"))
                                     page-paths)
        html-page-names         (map #(change-ext % 3 ".html") page-file-names)
        pages                   (map slurp page-paths)
        pages-data              (map parse-post pages)
        page-contents           (map :content pages-data)
        page-frontmatters       (map :frontmatter pages-data)
        page-formatted-contents (map md-to-html-string page-contents)
        page-titles             (map :title page-frontmatters)
        page-templates          (->> (map templates/page page-titles page-formatted-contents)
                                     (map templates/layout page-titles))

        post-paths              (reverse (md-files "./posts"))
        post-file-names         (map #(last (clojure.string/split % #"/"))
                                     post-paths)
        html-post-names         (map #(change-ext % 3 ".html") post-file-names)
        posts                   (map slurp post-paths)
        posts-data              (map parse-post posts)
        post-frontmatters       (map :frontmatter posts-data)
        post-contents           (map :content posts-data)
        post-formatted-contents (pmap (fn [s]
                                        (md-to-html-string
                                         s
                                         :code-style (fn [lang] (str "class=\"language-" lang "\""))))
                                      post-contents)
        post-titles             (map :title post-frontmatters)
        post-created-ats        (map :created post-frontmatters)
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

    (write! ["prism.css"] (vector (slurp "resources/prism.css" #_(io/resource "prism.css"))))
    (write! ["prism.js"] (vector (slurp "resources/prism.js" #_(io/resource "prism.js"))))
    (write! ["main.css"] [templates/stylesheet])
    (write! html-page-names page-templates)
    (write! html-post-names post-templates)
    (write! ["index.html"] [index-template])
    (write! ["feed"] [rss-feed])))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (build-blog)
  (shutdown-agents))
