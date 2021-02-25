(defproject cond-plus "1.1.0"
  :description "An adaption of the Racket cond macro for Clojure."
  :url "https://github.com/NoahTheDuke/cond-plus"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]]
  :repl-options {:init-ns cond-plus.core}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :sign-releases false}]])
