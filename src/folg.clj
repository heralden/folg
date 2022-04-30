(ns folg
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [markdown.core :as md]
            [stasis.core :as stasis]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.export]
            [juxt.dirwatch :refer [watch-dir]]))

(defn wrap-html [s]
  (str "<!DOCTYPE html>
       <html>
       <head>
         <meta charset=\"utf-8\">
         <style>
html {
  max-width: 70ch;
  padding: 3em 1em;
  margin: auto;
  line-height: 1.75;
  font-size: 1.25em;
}

h1,h2,h3,h4,h5,h6 {
  margin: 3em 0 1em;
  font-family: Helvetica, Arial, sans-serif;
  font-weight: 400;
}

p,ul,ol {
  margin-bottom: 2em;
  color: #1d1d1d;
  font-family: Georgia, Times, serif;
}

img {
  width: 100%;
  height: auto;
}
         </style>
         <title>Blogg</title>
       </head>
       <body>"
       s
       "</body>
       </html>"))

(def url-cmap
  {\space "%20"})

(defn append-images [md image-paths]
  (if (seq image-paths)
    (str md "\n"
         (->> (sort image-paths)
              (map #(str "![Photograph](." (str/escape % url-cmap) ")"))
              (str/join "\n")))
    md))

(defn get-related [path assets-data]
  (let [md-dir (drop-last (str/split path #"/"))]
    (keep (fn [{:keys [path]}]
            (let [dir (drop-last (str/split path #"/"))]
              (when (= md-dir dir)
                path)))
          assets-data)))

(comment
  (= (get-related "/foo/bar.md" [{:path "/foo/baz.jpg"} {:path "/bar/bog.jpg"}])
     ["/foo/baz.jpg"]))

(defn merge-pages [pages]
  {"/index.html"
   (->> (sort-by key #(compare %2 %1) pages)
        (map val)
        (str/join "<hr>"))})

(comment
  (= (merge-pages {"/a/path.html" "<p>text</p>"
                   "/b/path.html" "<p>more text</p>"})
     {"/index.html" "<p>text</p><hr><p>more text</p>"}))

(defn build [{:keys [out] :as _opts}]
  (let [target-dir (str out)
        assets-data (assets/load-assets "public" [#"(?i).+\.(jpg|png)"])
        assets (optimizations/all assets-data {})
        md-pages (stasis/slurp-directory "resources/public/" #"\.md$")
        pages (into {} (map (fn [[path md]]
                              [(str/replace path #"\.md$" ".html")
                               (-> (append-images md (get-related path assets-data))
                                   md/md-to-html-string wrap-html)]))
                    md-pages)]
    (stasis/empty-directory! target-dir)
    (optimus.export/save-assets assets target-dir)
    (stasis/export-pages (merge-pages pages) target-dir {:optimus-assets assets})))

(defn watch [opts]
  (build opts)
  (watch-dir (fn [_] (build opts)) (io/file "resources/public"))
  (while true))
