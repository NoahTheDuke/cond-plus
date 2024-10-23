(ns cond-plus.core-test
  (:require
   [cond-plus.core :refer [cond+]]
   [lazytest.core :refer [defdescribe expect it causes-with-msg?]]
   [lazytest.experimental.interfaces.clojure-test :refer [are deftest]]))

(defdescribe cond-branch
  (it "returns nil when given no input"
    (expect (nil? (cond+))))
  (it "returns nil when clauses are empty"
    (expect (nil? (cond+ [])))
    (expect (nil? (cond+ [] []))))
  (it "returns first body when associated test is true"
    (expect (= :first (cond+ [true :first])))
    (expect (= :first (cond+
                       [true :first]
                       [true :second]))))
  (it "returns nil when all tests are false"
    (expect (nil? (cond+ [false :first]))))
  (it "returns body when non-inital test is true"
    (expect (= :third
               (cond+
                [false :first]
                [nil :second]
                [true :third])))
    (expect (= :equal
               (cond+
                [(< 3 3) :greater]
                [(> 3 3) :less]
                [(= 3 3) :equal]))))
  (it "doesn't evaluate body of non-truthy branches"
    (expect (let [view (atom 0)]
              (cond+
               [false (swap! view inc)]
               [true :first])
              (zero? @view))))
  (it "evaluates body of first truthy branch"
    (let [view (atom 0)]
      (expect (= 1 (cond+
                    [true (swap! view inc)]
                    [true :first])))
      (expect (= 1 @view))))
  (it "evaluates test of each branch until truthy test is found"
    (let [view (atom true)]
      (expect (= :second
                 (cond+
                  [(swap! view not) :first]
                  [true :second])))
      (expect (false? @view))))
  (it "doesn't evaluate body of later truthy branches"
    (let [view (atom 0)]
      (expect (= :first
                 (cond+
                  [true :first]
                  [(swap! view inc) :second])))
      (expect (zero? @view))))
  (it "evaluation of body has an implicit do"
    (expect (= :second (cond+ [(< 1 2) :first :second])))
    (expect (= :fourth
               (cond+
                [(> 1 2) :first :second]
                [(= 1 1) :third :fourth])))
    (let [view (atom 0)]
      (expect (= :second
                 (cond+
                  [(> 1 2) :first]
                  [(= 1 1) (swap! view inc) :second])))
      (expect (= 1 @view))))
  (it "branches can use lists"
    (expect (= :equal
               (cond+
                ((< 3 3) :greater)
                ((> 3 3) :less)
                ((= 3 3) :equal)))))
  (it "branches can mix vectors and lists"
    (expect (= :equal
               (cond+
                [false :false]
                ((< 3 3) :greater)
                [(> 3 3) :less]
                ((= 3 3) :equal))))))

(defdescribe no-body-branch
  (it "returns first true test"
    (expect (= [1 2 3] (cond+ [(seq [1 2 3])])))
    (expect (= 1 (cond+ [1])))
    (expect (= 1 (cond+ [(first [1])])))
    (expect (= 1 (cond+
                  [false]
                  [1]))))
  (it "always evaluates body"
    (let [view (atom true)]
      (expect (= 1 (cond+
                    [(swap! view not)]
                    [1])))
      (expect (false? @view)))))

(defdescribe fn-branch
  (it "calls given function on result of first true test"
    (expect (= 1 (cond+ [[1 2 3] :> first])))
    (expect (= 2 (cond+ [(next [1 2 3]) :> first])))
    (expect (= [-2 -3] (cond+ [(next [1 2 3]) :> (fn [x] (map - x))])))
    (expect (= 2 (cond+ [(:key {:key 1}) :> inc])))
    (expect (= 2 (cond+
                  [(:key {:key 1}) :> inc]
                  [true true])))
    (expect (= 2 (cond+
                  [false 7]
                  [1 :> inc]))))
  (it "doesn't call given function when test returns false value"
    (expect (= false (cond+
                      [(:miss {:key 1}) :> inc]
                      [true false]))))
  (it "requires exactly 2 forms in body"
    (expect (causes-with-msg?
              java.lang.IllegalArgumentException
              #"bad :> clause"
              #(eval
                `(cond+ [1 :>]))))
    (expect (causes-with-msg?
              java.lang.IllegalArgumentException
              #"bad :> clause"
              #(eval
                `(cond+ [1 :> inc inc])))))
  (it "accepts symbol form"
    (expect (= 1 (cond+ [[1 2 3] => first])))))

(defdescribe else-branch
  (it "always returns body"
    (expect (= :first (cond+ [:else :first]))))
  (it "returns body when other branches are falsey"
    (expect (= :equal
               (cond+
                [(< 3 3) :greater]
                [(> 3 3) :less]
                [:else :equal]))))
  (it "evaluation of body has an implicit do"
    (expect (= :second (cond+ [:else :first :second])))
    (expect (= :second
               (cond+
                [false :first]
                [:else :equal :second])))
    (let [view (atom 0)]
      (expect (= :second
                 (cond+
                  [false :first]
                  [:else (swap! view inc) :second])))
      (expect (= 1 @view))))
  (it "must be in the final position"
    (expect (causes-with-msg?
              java.lang.IllegalArgumentException
              #":else not last"
              #(eval
                `(cond+
                  [false :first]
                  [:else :second]
                  [true :third]))))
    (expect (causes-with-msg?
              java.lang.IllegalArgumentException
              #":else not last"
              #(eval
                `(cond+
                  [:else :first]
                  [true :second])))))
  (it "must have a body"
    (expect (causes-with-msg?
             java.lang.IllegalArgumentException
              #"missing expression in :else clause"
              #(eval
                `(cond+
                  [false :first]
                  [:else])))))
  (it "accepts symbol form"
    (expect (= :first (cond+ [else :first])))))

(defdescribe combinations
  (it "Can combine different branch types"
    (let [view (atom true)]
      (expect (= :second
                 (cond+
                  [(< 3 3) :first]
                  [(:miss {:key 1}) :> inc]
                  [(swap! view not)]
                  [:else :second])))
      (expect (false? @view)))))

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
