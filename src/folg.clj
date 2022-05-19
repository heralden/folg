(ns folg
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [markdown.core :as md]
            [stasis.core :as stasis]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.export]
            [juxt.dirwatch :refer [watch-dir close-watcher]]))

(defn wrap-html [title s]
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

.post-date {
  font-size: 0.5em;
  opacity: 0.4;
}

.post-navigation {
  display: flex;
  justify-content: space-between;
}

.post-navigation > * {
  flex: 1;
  text-align: center;
}

.post-navigation > *:first-child {
  text-align: left;
}

.post-navigation > *:last-child {
  text-align: right;
}
         </style>
         <title>" title "</title>
       </head>
       <body>
         <h1>" title "</h1><hr>"
       s
       "</body>
       </html>"))

(def url-cmap
  {\space "%20"})

(defn append-images [md image-paths & {:keys [deploy-path]}]
  (if (seq image-paths)
    (str md "\n\n"
         (->> (sort image-paths)
              (map #(str "![Failed to load image](" deploy-path (str/escape % url-cmap) ")"))
              (str/join "\n")))
    md))

(comment
  (= (append-images "Foo" ["/foo/baz.jpg" "/foo/with spaces.png"])
     "Foo\n\n![Failed to load image](/foo/baz.jpg)\n![Failed to load image](/foo/with%20spaces.png)"))

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

(defn path->url
  "Remove everything after last slash, making it trailing."
  [filepath]
  (str/replace filepath #"/[^/]*$" "/"))

(defn create-toc [posts & {:keys [deploy-path]}]
  (str "<ul>"
       (str/join
         (map (fn [[path {:keys [metadata]}]]
                (str "<a href=\"" deploy-path (path->url path) "\"><li>" (first (:title metadata)) "</li></a>"))
              posts))
       "</ul>"))

(def get-meta-title (comp first :title))
(def get-meta-date (comp first :date))

(defn post->html [[_ {:keys [metadata html]}]]
  (str "<h2>" (get-meta-title metadata)
       "<span class=\"post-date\"><br>" (get-meta-date metadata) "</span>"
       "</h2>"
       html))

(defn wrap-navigation [{:keys [include-home no-top deploy-path]}
                       [prev-path {prev-meta :metadata}] [next-path {next-meta :metadata}] s]
  (let [nav (str "<div class=\"post-navigation\">"
                 "<span>"
                 (when prev-path
                   (str "« <a href=\"" deploy-path (path->url prev-path) "\">" (get-meta-title prev-meta) "</a>"))
                 "</span>"
                 (when include-home
                   (str "<a href=\"" (or deploy-path "/") "\">Home</a>"))
                 "<span>"
                 (when next-path
                   (str "<a href=\"" deploy-path (path->url next-path) "\">" (get-meta-title next-meta) "</a> »"))
                 "</span>"
                 "</div>")]
    (str (when-not no-top nav) s nav)))

(defn create-pages [pages title & {:keys [toc paginate deploy-path]}]
  (let [posts (sort-by (comp first :date :metadata val) #(compare %2 %1) pages)]
    (merge
      (cond

        toc
        (into {} (map (fn [[prev-post [path :as post] next-post]]
                        [(path->url path)
                         (->> (post->html post)
                              (wrap-navigation {:include-home true :deploy-path deploy-path}
                                               prev-post next-post)
                              (wrap-html title))]))
              (partition 3 1 (concat [nil] posts [nil])))

        paginate
        (let [paginated-posts (map-indexed vector (partition paginate paginate nil posts))]
          (into {} (map (fn [[index the-posts]]
                          [(str "/" index "/")
                           (->> (map post->html the-posts)
                                (str/join "<hr>")
                                (wrap-navigation {:no-top true :deploy-path deploy-path}
                                                 [(if (> index 1) (str "/" (dec index) "/") "/") {:metadata {:title ["Previous"]}}]
                                                 (when (< (inc index) (count paginated-posts))
                                                   [(str "/" (inc index) "/") {:metadata {:title ["Next"]}}]))
                                (wrap-html title))]))
                (drop 1 paginated-posts))))

      {"/index.html"
       (wrap-html title (cond
                          toc (str (create-toc posts :deploy-path deploy-path)
                                   (->> (post->html (first posts))
                                        (wrap-navigation {:no-top true :deploy-path deploy-path} nil (second posts))))
                          paginate (->> (take paginate posts)
                                        (map post->html)
                                        (str/join "<hr>")
                                        (wrap-navigation {:no-top true :deploy-path deploy-path} nil ["/1/" {:metadata {:title ["Next"]}}]))
                          :else (str/join "<hr>" (map post->html posts))))})))

(comment
  (create-pages {"/foo/path.html" {:metadata {:title ["Foo"]
                                              :date ["1990-01-01"]}
                                   :html "<p>text</p>"}
                 "/bar/path.html" {:metadata {:title ["Bar"]
                                              :date ["1990-01-02"]}
                                   :html "<p>more text</p>"}}
               "My title")
  {"/index.html" \space}

  (create-pages {"/foo/path.html" {:metadata {:title ["Foo"]
                                              :date ["1990-01-01"]}
                                   :html "<p>text</p>"}
                 "/bar/path.html" {:metadata {:title ["Bar"]
                                              :date ["1990-01-02"]}
                                   :html "<p>more text</p>"}}
               "My title"
               :toc true)
  {"/foo/" \space
   "/bar/" \space
   "/index.html" \space}

  (create-pages {"/foo/path.html" {:metadata {:title ["Foo"]
                                              :date ["1990-01-01"]}
                                   :html "<p>text</p>"}
                 "/bar/path.html" {:metadata {:title ["Bar"]
                                              :date ["1990-01-02"]}
                                   :html "<p>more text</p>"}}
               "My title"
               :paginate 1)
  {"/0/" \space
   "/1/" \space
   "/index.html" \space})

(defn get-file-names! []
  (keep #(when (.isFile %) (.getName %))
        (file-seq (io/file "resources/public"))))

(defn generate-site! [{:keys [out title toc paginate deploy-path] :as _opts}]
  (when (.exists (io/file "resources/public"))
    (let [target-dir (str out)
          title (str (or title "Blog"))
          file-names (get-file-names!)
          assets-data (when (some #(re-find #"(?i).+\.(jpg|png)" %) file-names)
                        (assets/load-assets "public" [#"(?i).+\.(jpg|png)"]))
          assets (optimizations/all assets-data {})
          md-pages (when (some #(re-find #"\.md$" %) file-names)
                     (stasis/slurp-directory "resources/public/" #"\.md$"))
          pages (into {} (map (fn [[path md]]
                                [(str/replace path #"\.md$" ".html")
                                 (-> (append-images md (get-related path assets-data) :deploy-path deploy-path)
                                     (md/md-to-html-string-with-meta))]))
                      md-pages)]
      (stasis/empty-directory! target-dir)
      (optimus.export/save-assets assets target-dir)
      (stasis/export-pages (create-pages pages title :toc toc :paginate paginate :deploy-path deploy-path)
                           target-dir
                           {:optimus-assets assets}))))

(defn validate-opts! [opts]
  (when (str/blank? (:out opts))
    (println "Please specify a path for :out")
    (System/exit 1))
  (when (and (some? (:paginate opts))
             (not (integer? (:paginate opts))))
    (println ":paginate can only be a whole number")
    (System/exit 1)))

(defn build [opts]
  (validate-opts! opts)
  (generate-site! opts))

(defn watch [opts]
  (validate-opts! opts)
  (generate-site! opts)
  (let [watcher (watch-dir (fn [_] (generate-site! opts)) (io/file "resources/public"))]
    (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (close-watcher watcher)))))
  (while true
    (Thread/sleep 5000)))
