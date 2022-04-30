(ns folg.core
  (:require [clojure.string :as str]
            [markdown.core :as md]
            [stasis.core :as stasis]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.export]))

(def target-dir "out")

(defn wrap-html [s]
  (str "<!DOCTYPE html>
       <html>
       <head>
         <meta charset=\"utf-8\">
         <link href=\"/styles/folg.css\" rel=\"stylesheet\">
         <title>Blogg</title>
       </head>
       <body>"
       s
       "</body>
       </html>"))

(defn export []
  (let [assets (optimizations/all (assets/load-assets "public" ["/styles/folg.css"
                                                                #"(?i)/photos/.+\.(jpg|png)"])
                                  {})
        md-pages (stasis/slurp-directory "resources/public/" #"\.md$")
        pages (into {} (map (fn [[path md :as kv]]
                              [(str/replace path #"\.md$" ".html")
                               (wrap-html (md/md-to-html-string md))]))
                    md-pages)]
    (stasis/empty-directory! target-dir)
    (optimus.export/save-assets assets target-dir)
    (stasis/export-pages pages target-dir {:optimus-assets assets})))

(comment
  (export))
