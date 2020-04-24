(ns cond-plus.core-test
  (:require [clojure.test :refer :all]
            [cond-plus.core :refer :all]))

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
  (testing "don't evaluate body of non-truthy branches"
    (is (let [view (atom 0)]
          (cond+
            [false (swap! view inc)]
            [true :first])
          (zero? @view))))
  (testing "evaluate body of first truthy branch"
    (let [view (atom 0)]
      (is (= 1 (cond+
                 [true (swap! view inc)]
                 [true :first])))
      (is (= 1 @view))))
  (testing "evaluate test of each branch until truthy test is found"
    (let [view (atom true)]
      (is (= :second
             (cond+
               [(swap! view not) :first]
               [true :second])))
      (is (false? @view))))
  (testing "don't evaluate body of later truthy branches"
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
      (is (false? @view)))))

(defn illegal-argument-validator
  [form]
  (try (eval form)
       (catch clojure.lang.Compiler$CompilerException e (throw (.getCause e)))))

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
  (testing "requires exactly 2 forms in body"
    (is (thrown? java.lang.IllegalArgumentException
                 (illegal-argument-validator
                   '(cond-plus.core/cond+ [1 :>]))))
    (is (thrown? java.lang.IllegalArgumentException
                 (illegal-argument-validator
                   '(cond-plus.core/cond+ [1 :> inc inc])))))
  (testing "accepts symbol form"
    (is (= 1 (cond+ [[1 2 3] => first])))))

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
  (testing "must be in the final position"
    (is (thrown? java.lang.IllegalArgumentException
                 (illegal-argument-validator
                   '(cond-plus.core/cond+
                      [false :first]
                      [:else :second]
                      [true :third]))))
    (is (thrown? java.lang.IllegalArgumentException
                 (illegal-argument-validator
                   '(cond-plus.core/cond+
                      [:else :first]
                      [true :second])))))
  (testing "must have a body"
    (is (thrown? java.lang.IllegalArgumentException
                 (illegal-argument-validator
                   '(cond-plus.core/cond+
                      [false :first]
                      [:else])))))
  (testing "accepts symbol form"
    (is (= :first (cond+ [else :first])))))

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
      2/3
      0M 1M
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

    (cond+ [true 1] [true (throw (new Exception "Exception which should never occur"))]) 1
    (cond+ [nil 1] [false 2] [true 3] [true 4]) 3
    (cond+ [nil 1] [false 2] [true 3] [true (throw (new Exception "Exception which should never occur"))]) 3)

  ; false
  (are [x]  (= (cond+ [x :a] [true :b]) :b)
    nil false )

  ; true
  (are [x]  (= (cond+ [x :a] [true :b]) :a)
    true
    0 42
    0.0 3.14
    2/3
    0M 1M
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
    (cond+ [(> 3 2) (+ 1 2)] [true :result] [true (throw (new Exception "Exception which should never occur"))]) 3
    (cond+ [(< 3 2) (+ 1 2)] [true :result] [true (throw (new Exception "Exception which should never occur"))]) :result)

  ; identity (= (cond+ [true x]) x)
  (maintains-identity (fn [x] (cond+ [true x]))))
