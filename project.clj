(defproject cond-plus "1.1.1"
  :description "An adaption of the Racket cond macro for Clojure."
  :url "https://github.com/NoahTheDuke/cond-plus"
  :license {:name "MPL-2.0"
            :url "https://www.mozilla.org/en-US/MPL/2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]]
  :repl-options {:init-ns cond-plus.core}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :sign-releases false}]])
