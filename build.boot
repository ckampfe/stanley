(def project 'stanley)
(def version "0.0.3")

(set-env! :resource-paths #{"resources" "src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "1.9.0-alpha14"]
                            [org.clojure/test.check "0.9.0" :scope "dev"]
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

(deftask publish
  "Publish to the blog."
  [u user       VAL str "The user on the host machine"
   i host       VAL str "The host machine, ie foo.com"
   d target-dir VAL str "The target dir, ie ~"]
  (let [target-dir (if (seq target-dir) target-dir "~")
        cwd (->> (clojure.java.shell/sh "pwd") :out (clojure.string/trim-newline))
        jar-location (str cwd "/target/stanley-" version "-standalone.jar")]

    (println (:err (clojure.java.shell/sh "time"
                                          "java"
                                          "-jar"
                                          jar-location)))
    (println "built site")

    (clojure.java.shell/sh "tar"
                           "-czf"
                           "./build.tgz"
                           "build")
    (println "created tarball at build.tgz")

    (clojure.java.shell/sh "scp"
                           "build.tgz"
                           (str user "@" host ":" target-dir))
    (println "scp'd build.tgz to" (str user "@" host ":" target-dir))

    (println (:out (clojure.java.shell/sh
                    "ssh"
                    "-t"
                    (str user
                         "@"
                         host)
                    (str "hostname; cd " target-dir "; tar -xvf build.tgz; sudo cp -r build/* /usr/share/nginx/www; cd ~; exit"))))))

(require '[adzerk.boot-test :refer [test]])
