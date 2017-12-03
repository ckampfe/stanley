(def project 'stanley)
(def version "0.0.3")

(set-env! :resource-paths #{"src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "1.9.0-RC2"]
                            [org.clojure/spec.alpha "0.1.123"]
                            [org.clojure/test.check "0.10.0-alpha2" :scope "dev"]
                            [markdown-clj "1.0.1"]
                            [clj-rss "0.2.3"]
                            [hiccup "1.0.5"]
                            [garden "1.3.3"]
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

(defn seconds-from [time]
  (let [finish-time (java.time.Instant/now)]
    (/ (->> time
            .toEpochMilli
            (.minusMillis finish-time)
            .toEpochMilli)
       1000.0)))

(deftask publish
  "Publish to the blog."
  [u user       VAL str "The user on the host machine"
   i host       VAL str "The host machine, ie foo.com"
   d target-dir VAL str "The target dir, ie ~"]
  (let [target-dir (if (seq target-dir) target-dir "~")
        start-time (java.time.Instant/now)]

    (run)
    (println "built site in" (seconds-from start-time) "seconds")

    (clojure.java.shell/sh "tar"
                           "-czf"
                           "./build.tgz"
                           "build")
    (println "created tarball at build.tgz")

    (println (clojure.java.shell/sh "scp"
                                    "build.tgz"
                                    (str user "@" host ":" target-dir)))
    (println "scp'd build.tgz to" (str user "@" host ":" target-dir))

    (println (:out (clojure.java.shell/sh
                    "ssh"
                    "-t"
                    (str user
                         "@"
                         host)
                    (str "hostname; cd " target-dir "; tar -xvf build.tgz; sudo cp -r build/* /usr/share/nginx/www; cd ~; exit"))))))

(require '[adzerk.boot-test :refer [test]])
