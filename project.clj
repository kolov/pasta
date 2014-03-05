(defproject pasta "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                  [org.clojure/clojure "1.5.1"]
                  [org.clojure/clojurescript "0.0-2173"]
                  [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                  ]
  :plugins [[lein-cljsbuild "1.0.2"]]
  :cljsbuild {:builds [{:source-paths ["cljs"],
                        :compiler     {:pretty-print  true,
                                       :output-to     "web/pasta.js",
                                       :optimizations :simple
                                       }}
                       ]
              }
  )
