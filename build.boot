(def project 'stanley)
(def version "0.0.1")

(set-env! :resource-paths #{"resources" "src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "1.8.0"]
                            [markdown-clj "0.9.89"]
                            [clj-rss "0.2.3"]
                            [hiccup "1.0.5"]
                            [garden "1.3.2"]
                            [adzerk/boot-test "RELEASE" :scope "test"]])

(task-options!
 aot {:namespace   #{'stanley.core}}
 pom {:project     project
      :version     version
      :description "build my site"
      :url         "http://zeroclarkthirty.com/"
      :scm         {:url "https://github.com/ckampfe/stanley"}
      :license     {"GNU Affero General Public License 3.0"
                    "http://choosealicense.com/licenses/agpl-3.0/"}}
 jar {:main        'stanley.core
      :file        (str "stanley-" version "-standalone.jar")})

(deftask build
  "Build the project locally as a JAR."
  [d dir PATH #{str} "the set of directories to write to (target)."]
  (let [dir (if (seq dir) dir #{"target"})]
    (comp (aot) (pom) (uber) (jar) (target :dir dir))))

(deftask run
  "Run the project."
  [a args ARG [str] "the arguments for the application."]
  (require '[stanley.core :as app])
  (apply (resolve 'app/-main) args))

(require '[adzerk.boot-test :refer [test]])
