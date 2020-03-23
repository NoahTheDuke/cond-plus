(ns cond-plus.core-test
  (:require [clojure.test :refer :all]
            [cond-plus.core :refer :all]))

(deftest cond+-test
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
             [true :third]))))
  (testing "returns body when non-inital test is true"
    (is (= :equal
           (cond+
             [(< 3 3) :greater]
             [(> 3 3) :less]
             [(= 3 3) :equal]))))
  (testing "don't run body of non-truthy branches"
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
  (testing "evaluation of body has an implicit do"
    (is (= :second (cond+ [(< 1 2) :first :second])))
    (is (= :second
           (cond+
             [(> 1 2) :ok]
             [(= 1 1) :first :second])))
    (let [view (atom 0)]
      (is (= :second
             (cond+
               [(> 1 2) :ok]
               [(= 1 1) (swap! view inc) :second])))
      (is (= 1 @view))))
  (testing "branches can use vectors or lists"
    (is (= :equal
           (cond+
             ((< 3 3) :greater)
             ((> 3 3) :less)
             ((= 3 3) :equal)))))
  (testing "=>"
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
    (testing "accepts symbol form"
      (is (= 1 (cond+ [[1 2 3] => first])))))
  (testing "no body given"
    (testing "returns first true test"
      (is (= [1 2 3] (cond+ [(seq [1 2 3])])))
      (is (= 1 (cond+ [1])))
      (is (= 1 (cond+ [(first [1])])))
      (is (= 1 (cond+ [false] [1]))))
    (testing "always evaluates body"
      (let [view (atom true)]
        (is (= 1 (cond+
                   [(swap! view not)]
                   [1])))
        (is (false? @view)))))
  (testing "else"
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
               [(< 3 3) :greater]
               [(> 3 3) :less]
               [:else :equal :second])))
      (let [view (atom 0)]
        (is (= :second
               (cond+
                 [false :ok]
                 [:else (swap! view inc) :second])))
        (is (= 1 @view))))
    (testing "accepts symbol form"
      (is (= :first (cond+ [else :first]))))))

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
