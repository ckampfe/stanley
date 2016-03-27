(ns stanley.templates
  (:use [hiccup.core]
        [hiccup.page :refer [html5]]))

(def stylesheet "
/*****************************************************************************/
/*
/* Common
/*
/*****************************************************************************/

/* Global Reset */


* {
  margin: 0;
  padding: 0;
  color: #444;
}

html, body { height: 100%; }

body {
  background-color: #F9F9F9;
  font: 13.34px monospace;
  font-size: small;
  text-align: center;
}

h1, h2, h3, h4, h5, h6 {
  font-size: 100%; }

h1 { margin-bottom: 1em; }
p { margin: 1em 0; }

a         { color: #8085C1; text-decoration: none; }
a:hover   { color: #636B96; text-decoration: underline }
// a:visited { color: gray; }

/*****************************************************************************/
/*
/* Home
/*
/*****************************************************************************/
ul.posts {
  list-style-type: none;
  margin-bottom: 2em;
}

//post titles
ul.posts li {
  line-height: 1.75em;
}

// date
ul.posts span {
  color: #aaa;
  font-family: \"Courier New\", monospace, Monaco;
  font-size: 80%;
}

li span {
  float: right;
  color: gray;
}

/*****************************************************************************/
/*
/* Site
/*
/*****************************************************************************/



.site {
  font-size: 115%;
  text-align: justify;
  max-width: 44em;
  margin: 3em auto 2em;
  padding: 0 7% 0;
  line-height: 1.5em;
  display: inline-block;
}

.site #home {
  min-width: 44em;
}

.site .header a {
  font-weight: bold;
  text-decoration: none;
}

.site .header h1.title {
  display: inline-block;
  margin-bottom: 2em;
}

.site .header h1.title a {
  font-size: 3em;
  margin-left: -1em;
  color: #F9F9F9;
  background-color: #FF96A7;
  padding: 0.06em 0.06em 0.05em 0.06em;
}

.site .header h1.title a:hover {
  color: gray;
  background-color: #F9F9F9;
}

.site .header a.extra {
  color: #aaa;
  margin-left: 1.3em;
}

.site .header a.extra:hover {
  color: #000;
}

.site .meta {
  color: #aaa;
}

.site #selfie {
  width: 50%;
}

.site .footer {
  font-size: 85%;
  color: #C8C8C8;
  border-top: 4px solid #eee;
  margin-top: 2em;
  overflow: hidden;
}

.site .footer .contact {
  float: right;
  margin-right: 2em;
}

.site .footer .contact a {
  color: #9FA6F0;
  padding: 0 0 0 8px;
  margin: 0 0 0 auto;
}

.site .footer .contact a:hover {
  color: #636B96;
}

.site .footer p {
  display: inline-flex;
}

.site .footer .rss {
  margin-top: 1.1em;
  margin-right: -.2em;
  float: right;
}

.site .footer .rss img {
  border: 0;
}

/*****************************************************************************/
/*
/* Posts
/*
/*****************************************************************************/

.post a {
  color: white;
  background-color: #FF96A7;
  text-decoration: none;
  padding: 1px;
}

.post a:hover {
  color: gray;
  background-color: #F9F9F9;

.post ul, .post ol {
  margin-left: 1.35em;
}")

(defn page [title content]
  (html [:h1 title]
        [:div {:class "page"} content]))

(defn post [title created content]
  (html [:h2 title]
        [:p {:class "meta"} created]
        [:div {:class "post"} content]))

(defn index [post-paths titles created-ats]
  (letfn [(link [filename title created-at]
            [:li
             [:a {:href filename} title]
             [:span created-at]])]

    (html [:div {:id "home"}
           [:ul {:class "posts"}
            (map link post-paths titles created-ats)]])))

(defn layout [title content]
  (html5 [:head
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
          [:title (if title
                    (str title " - Clark Kampfe"))]
          [:meta {:name "viewport" :content "width=device-width"}]
          [:link {:rel "stylesheet" :href "main.css"}]
          [:link {:rel "icon" :type "image/png" :href "/favicon"}]]

         [:body
          [:div {:class "container"}
           [:div {:class "site"}
            [:div {:class "header"}
             [:h1 {:class "title"}
              [:a {:href "index.html"} "Clark Kampfe"]]
             [:a {:class "extra" :href "about.html"} "about"]
             [:a {:class "extra" :href "resume.html"} "resumé"]]

            content

            [:div {:class "footer"}
             [:div {:class "contact"}
              [:p
               [:a {:href "http://github.com/ckampfe/"} "github"]
               [:a {:href "http://twitter.com/clarkkampfe"} "twitter"]
               [:br]]]]]]]))
