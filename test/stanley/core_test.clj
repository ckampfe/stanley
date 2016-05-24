(ns stanley.core-test
  (:require [clojure.test :refer :all]
            [stanley.core :refer :all]))

(deftest get-content-test
  (let [frontmatter "---\ntitle: some title\ncreated: a date\nfoo: bar\n---\n"
        post-body "some content"
        post (str frontmatter post-body)]

    (testing "it gets content"
      (is (= (get-content post) post-body)))

    (testing "it does not get frontmatter"
      (is (not (clojure.string/starts-with?
                (get-content post)
                frontmatter))))))

(deftest get-frontmatter-test
  (let [frontmatter "---\ntitle: some title\ncreated: a date\nfoo: bar\n---\n"
        post-body "some content"
        post (str frontmatter post-body)
        bullshit-post (str frontmatter post-body "\n--------------\n" post-body)]

    (testing "it gets frontmatter"
      (is (map? (get-frontmatter post)))
      (is (= (keys (get-frontmatter post))
             (list "title" "created" "foo")))
      (is (= (vals (get-frontmatter post))
             (list "some title" "a date" "bar"))))

    (testing "it does not get content"
      (is (not (clojure.string/starts-with?
                (get-frontmatter post)
                post-body))))

    (testing "it does not get content with extra -------"
      (is (not (clojure.string/starts-with?
                (get-frontmatter bullshit-post)
                bullshit-post))))))

(deftest change-ext-test
  (let [filename "foo.html"]
    (testing "it change the extension"
      (is (= (change-ext filename 4 "md") "foo.md")))))

(deftest to-build-dir-test
  (testing "it adds the build dir"
    (binding [build-dir "build"]
      (is (= (to-build-dir "foo.md")
             "build/foo.md")))))
