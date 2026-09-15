; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.

(ns com.noahbogart.clj-kondo-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.hooks-api :as hooks-api]
   [clojure.string :as str]
   [lazytest.core :refer [defdescribe expect-it]]
   [lazytest.extensions.matcher-combinators :refer [match?]]))

(defn- lint [& lines]
  (let [code (->> (into ["(ns user (:require [com.noahbogart.cond-plus :refer [cond+]]))"] lines)
                  (str/join "\n"))]
    (:findings (binding [hooks-api/*reload* true]
                 (with-in-str code (clj-kondo/run! {:config {:output {:format :edn}}
                                                    :lint ["-"]}))))))

(defdescribe lints
  (expect-it "works in genera"
    (empty? (lint "(cond+ [true 1])")))
  (expect-it "clauses must be list or vector"
    (match? [{:type :cond-plus/sequence
              :row 2
              :message "must be sequence"}]
            (lint "(cond+ {1 2})")))
  (expect-it "catch missing fns"
    (match? [{:type :cond-plus/missing-fn
              :row 2
              :message "fn-expr must have third position symbol"}]
            (lint "(cond+ [1 :>])")))
  (expect-it "catch too many fns"
    (match? [{:type :cond-plus/bad-fn
              :row 2
              :message "too many args given to fn-expr"}]
            (lint "(cond+ [1 :> inc inc])")))
  (expect-it "else not last"
    (match? [{:type :cond-plus/non-final-else
              :row 2
              :message ":else must be in final position"}]
            (lint "(cond+ [false :first] [:else :second] [true :third])")))
  (expect-it "else without body"
    (match? [{:type :cond-plus/empty-else
              :row 2
              :message ":else must have a body"}]
            (lint "(cond+ [false :first] [:else])")))
  (expect-it "else allows recur"
    (empty?
     (lint "(defn hello [a] (cond+ [(pos? a) :> (fn [x] (* x 2))] [:else (recur (inc a))]))")))
  (expect-it "else allows body of nil"
    (empty?
     (lint "(cond+ [false :first] [:else nil])")))
  (expect-it "branch doesn't allow recur"
    (empty?
     (lint "(defn hello [a] (cond+ [(recur (inc a))]))"))))
