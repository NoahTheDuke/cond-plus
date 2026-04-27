; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.

(ns com.noahbogart.cond-plus-test
  (:require
   [com.noahbogart.cond-plus :refer [=> cond+ else]]
   #?(:clj [lazytest.experimental.interfaces.clojure-test :refer [are deftest testing is]]
      :cljs [clojure.test :refer [are deftest testing is]]))
  #?(:clj (:import
           [clojure.lang ExceptionInfo])))

;; this is borrowed from lazytest
(defn ^:no-doc cause-seq
  [throwable]
  (lazy-seq
   (when (instance? #?(:clj Throwable :cljs js/Error) throwable)
     (cons throwable (lazy-seq (cause-seq (ex-cause throwable)))))))

(defn causes?
  [c f]
  (try (f) false
       (catch #?(:clj Throwable :cljs :default) t
         (if (some #(instance? c %) (cause-seq t))
           true
           (throw t)))))

(defn causes-with-msg?
  [c re f]
  (try (f) false
       (catch #?(:clj Throwable :cljs :default) t
         (let [causes (cause-seq t)]
           (cond
             (some (fn causes-with-msg?-wrapper [cause]
                     (when
                       (and (instance? c cause)
                            (re-find re (ex-message cause)))
                       c))
                   causes) true
             (some #(instance? c %) causes)
             (let [found (some #(when (instance? c %) %) causes)
                   msg #?(:clj (format "%s found but not with expected message"
                                       (.getName ^Class c))
                          :cljs (str (type c) "found but not with expected message"))]
               (throw (ex-info msg {:found found})))
             :else
             (throw t))))))

(deftest cond-branch
  (testing "returns nil when given no input"
    (is (nil? (cond+))))
  (testing "returns nil when clauses are empty"
    (is (nil? (cond+ [])))
    (is (nil? (cond+ [] []))))
  (testing "returns first body when associated test is true"
    (is (= :first (cond+ [true :first])))
    (is (= :first (cond+
                       [true :first]
                       [true :second]))))
  (testing "returns nil when all tests are false"
    (is (nil? (cond+ [false :first]))))
  (testing "returns body when non-inital test is true"
    (is (= :third
               (cond+
                [false :first]
                [nil :second]
                [true :third])))
    (is (= :equal
               (cond+
                [(< 3 3) :greater]
                [(> 3 3) :less]
                [(= 3 3) :equal]))))
  (testing "doesn't evaluate body of non-truthy branches"
    (is (let [view (atom 0)]
              (cond+
               [false (swap! view inc)]
               [true :first])
              (zero? @view))))
  (testing "evaluates body of first truthy branch"
    (let [view (atom 0)]
      (is (= 1 (cond+
                    [true (swap! view inc)]
                    [true :first])))
      (is (= 1 @view))))
  (testing "evaluates test of each branch until truthy test is found"
    (let [view (atom true)]
      (is (= :second
                 (cond+
                  [(swap! view not) :first]
                  [true :second])))
      (is (false? @view))))
  (testing "doesn't evaluate body of later truthy branches"
    (let [view (atom 0)]
      (is (= :first
                 (cond+
                  [true :first]
                  [(swap! view inc) :second])))
      (is (zero? @view))))
  (testing "evaluation of body has an implicit do"
    (is (= :second (cond+ [(< 1 2) :first :second])))
    (is (= :fourth
               (cond+
                [(> 1 2) :first :second]
                [(= 1 1) :third :fourth])))
    (let [view (atom 0)]
      (is (= :second
                 (cond+
                  [(> 1 2) :first]
                  [(= 1 1) (swap! view inc) :second])))
      (is (= 1 @view))))
  (testing "branches can use lists"
    (is (= :equal
               (cond+
                ((< 3 3) :greater)
                ((> 3 3) :less)
                ((= 3 3) :equal)))))
  (testing "branches can mix vectors and lists"
    (is (= :equal
               (cond+
                [false :false]
                ((< 3 3) :greater)
                [(> 3 3) :less]
                ((= 3 3) :equal))))))

(deftest no-body-branch
  (testing "returns first true test"
    (is (= [1 2 3] (cond+ [(seq [1 2 3])])))
    (is (= 1 (cond+ [1])))
    (is (= 1 (cond+ [(first [1])])))
    (is (= 1 (cond+
                  [false]
                  [1]))))
  (testing "always evaluates body"
    (let [view (atom true)]
      (is (= 1 (cond+
                    [(swap! view not)]
                    [1])))
      (is (false? @view))))
  #?(:clj (testing "rejects recur"
            (is (causes-with-msg?
                  clojure.lang.Compiler$CompilerException
                  #"Syntax error"
                  #(eval `(defn ~'hello [a#]
                            (cond+ [(recur (inc a#))]))))))))

(deftest fn-branch
  (testing "calls given function on result of first true test"
    (is (= 1 (cond+ [[1 2 3] :> first])))
    (is (= 2 (cond+ [(next [1 2 3]) :> first])))
    (is (= [-2 -3] (cond+ [(next [1 2 3]) :> (fn [x] (map - x))])))
    (is (= 2 (cond+ [(:key {:key 1}) :> inc])))
    (is (= 2 (cond+
                  [(:key {:key 1}) :> inc]
                  [true true])))
    (is (= 2 (cond+
                  [false 7]
                  [1 :> inc]))))
  (testing "doesn't call given function when test returns false value"
    (is (= false (cond+
                      [(:miss {:key 1}) :> inc]
                      [true false]))))
  #?(:clj (testing "requires exactly 2 forms in body"
            (is (causes-with-msg?
                  clojure.lang.ExceptionInfo
                  #"bad :> clause"
                  #(eval
                    `(cond+ [1 :>]))))
            (is (causes-with-msg?
                  clojure.lang.ExceptionInfo
                  #"bad :> clause"
                  #(eval
                    `(cond+ [1 :> inc inc]))))))
  (testing "accepts symbol form"
    (is (= 1 (cond+ [[1 2 3] => first]))))
  (testing "allows recur"
    (is (nil? (#(cond+ [true :> (fn [b#] (when b# (recur (not b#))))]))))
    (is (= 1 (cond+ [0 :> (fn [b] (if (zero? b) (recur (inc b)) b))])))))

(deftest else-branch
  (testing "always returns body"
    (is (= :first (cond+ [:else :first]))))
  (testing "returns body when other branches are falsey"
    (is (= :equal
               (cond+
                [(< 3 3) :greater]
                [(> 3 3) :less]
                [:else :equal]))))
  (testing "evaluation of body has an implicit do"
    (is (= :second (cond+ [:else :first :second])))
    (is (= :second
               (cond+
                [false :first]
                [:else :equal :second])))
    (let [view (atom 0)]
      (is (= :second
                 (cond+
                  [false :first]
                  [:else (swap! view inc) :second])))
      (is (= 1 @view))))
  #?(:clj (testing "must be in the final position"
            (is (causes-with-msg?
                  ExceptionInfo
                  #":else not last"
                  #(eval
                    `(cond+
                      [false :first]
                      [:else :second]
                      [true :third]))))
            (is (causes-with-msg?
                  ExceptionInfo
                  #":else not last"
                  #(eval
                    `(cond+
                      [:else :first]
                      [true :second]))))))
  #?(:clj (testing "must have a body"
            (is (causes-with-msg?
                  ExceptionInfo
                  #"missing expression in :else clause"
                  #(eval
                    `(cond+
                      [false :first]
                      [:else]))))))
  (testing "accepts symbol form"
    (is (= :first (cond+
                       [false :zero]
                       [else :first]))))
  #?(:clj (testing "accepts recur"
            (is (#(eval `(defn ~'hello [a#]
                           (cond+ [(pos? a#) :> (fn [x#] (* x# 2))]
                                  [:else (recur (inc a#))]))))))))

(deftest combinations
  (testing "Can combine different branch types"
    (let [view (atom true)]
      (is (= :second
                 (cond+
                  [(< 3 3) :first]
                  [(:miss {:key 1}) :> inc]
                  [(swap! view not)]
                  [:else :second])))
      (is (false? @view)))))

;; Adapted from Clojure Core
(defn maintains-identity [f]
  (are [x] (= (f x) x)
    nil
    false true
    0 42
    0.0 3.14
    #?(:clj 2/3)
    #?@(:clj [0M 1M])
    \c
    "" "abc"
    'sym
    :kw
    () '(1 2)
    [] [1 2]
    {} {:a 1 :b 2}
    #{} #{1 2} ))

(deftest original-cond-test
  (are [x y] (= x y)
    (cond+) nil

    (cond+ [nil true]) nil
    (cond+ [false true]) nil

    (cond+ [true 1] [true (throw (ex-info "Exception which should never occur" {}))]) 1
    (cond+ [nil 1] [false 2] [true 3] [true 4]) 3
    (cond+ [nil 1] [false 2] [true 3] [true (throw (ex-info "Exception which should never occur" {}))]) 3)

  ; false
  (are [x]  (= (cond+ [x :a] [true :b]) :b)
    nil false )

  ; true
  (are [x]  (= (cond+ [x :a] [true :b]) :a)
    true
    0 42
    0.0 3.14
    #?(:clj 2/3)
    #?@(:clj [0M 1M])
    \c
    "" "abc"
    'sym
    :kw
    () '(1 2)
    [] [1 2]
    {} {:a 1 :b 2}
    #{} #{1 2} )

  ; evaluation
  (are [x y] (= x y)
    (cond+ [(> 3 2) (+ 1 2)] [true :result] [true (throw (ex-info "Exception which should never occur" {}))]) 3
    (cond+ [(< 3 2) (+ 1 2)] [true :result] [true (throw (ex-info "Exception which should never occur" {}))]) :result)

  ; identity (= (cond+ [true x]) x)
  (maintains-identity (fn [x] (cond+ [true x]))))
