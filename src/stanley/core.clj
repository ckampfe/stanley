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
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [stanley.templates :as templates]
            [stanley.rss :as rss]
            [markdown.core :as md :refer [md-to-html-string]])
  (:import [java.io File])
  (:gen-class))

(def ^:dynamic build-dir "build")

(defn files
  "Given a `dir', list the files,
  removing hidden files and directories, mapping files to
  their canonical path"
  [dir]
  (->> (File. dir)
       (.listFiles)
       (remove #(.isHidden %))
       (remove #(.isDirectory %))
       (map #(.getCanonicalPath %))))

(defn md-files
  "fetch files in `dir' and select only the ones that
  end in `.md'"
  [dir]
  (->> (files dir)
       (filter #(clojure.string/ends-with? % ".md"))))

(def gen-post
  "a generator to create a post, which is a frontmatter and a body"
  (gen/fmap (fn [[s1 s2 s3 s4]] (str "---\n"
                                     "title: "   s1 "\n"
                                     "created: " s2 "\n"
                                     "layout: "  s3 "\n"
                                     "---\n"
                                     s4))
            (gen/tuple (gen/not-empty (gen/string-alphanumeric))
                       (gen/not-empty (gen/string-alphanumeric))
                       (gen/not-empty (gen/string-alphanumeric))
                       (gen/not-empty (gen/string-alphanumeric)))))

(s/def ::filename (s/and string?
                         #(not= % "")))

(def frontmatter-regex #"(?s)-{3}(.+)\n-{3}\n.+")

(s/def ::post-string (s/with-gen (s/and #(not= % "")
                                        #(re-matches frontmatter-regex %))
                       (fn [] gen-post)))

(s/fdef get-content
        :args (s/cat ::post-string ::post-string)
        :ret (s/and string?
                    #(not= % "")))

(defn get-content
  "Given a `post-string', split on newlines and drop
  the first 5 lines, joining the resulting collection into
  a string."
  [post-string]
  (->> post-string
       (#(clojure.string/split % #"\n"))
       (drop 5)
       (clojure.string/join "\n")))

(s/fdef get-frontmatter
        :args (s/cat ::post-string ::post-string)
        :ret (s/keys :unreq [:title :created]
                     :unopt [:layout]))

(defn get-frontmatter
  "Given a `post-string', return a map of the colon-separated
  frontmatter."
  [post-string]
  (let [[_ fm] (re-find frontmatter-regex post-string)]
    (->> (clojure.string/split fm #"\n")
         (remove clojure.string/blank?)
         (map #(clojure.string/split % #": "))
         (into {})
         (reduce-kv (fn [result k v]
                      (assoc result
                             (keyword k)
                             v))
                    {}))))

(s/fdef change-ext
        :args (s/cat ::filename ::filename
                     ::n (s/and integer?
                                (fn [n] (>= 0 n)))
                     ::replacement ::filename)
        :ret string?
        :fn #(s/and (clojure.string/includes? (:ret %)
                                              (::filename (:args %)))
                    (clojure.string/includes? (:ret %)
                                              (::replacement (:args %)))))

(defn change-ext
  "Given a filename, the length of an extension (including the dot),
  and a new extension, return a file with the new extension."
  [filename n replacement]
  (str (clojure.string/join
        (drop-last n filename)) replacement))

(s/fdef to-build-dir
        :args (s/cat ::filename ::filename)
        :ret (s/and #(clojure.string/includes? % "/")
                    string?)
        :fn (s/spec #(= (str build-dir "/" (::filename (:args %)))
                        (:ret %))))

(defn to-build-dir [filename] (str build-dir "/" filename))

(defn write!
  "Eagerly write the seq of contents to the seq of names."
  [names contents]
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
