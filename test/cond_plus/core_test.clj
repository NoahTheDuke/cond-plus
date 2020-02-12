(ns cond-plus.core-test
  (:require [clojure.test :refer :all]
            [cond-plus.core :refer :all]))

; (test 'greater 'cond (cond ((> 3 2) 'greater)
;                            ((< 3 2) 'less)))
; (test 'equal 'cond (cond ((> 3 3) 'greater)
;                          ((< 3 3) 'less)
;                          (else 'equal)))
; (test 2 'cond
;       (cond ((assv 'b '((a 1) (b 2))) => cadr)
;             (else #f)))
; (test #f 'cond (cond ((assv 'z '((a 1) (b 2))) => cadr)
;                      (else #f)))
; (syntax-test #'(cond ((assv 'z '((a 1) (b 2))) => cadr)
;                      (else 8)
;                      (else #f)))
; (test #f 'cond (let ([else #f])
;                  (cond ((assv 'z '((a 1) (b 2))) => cadr)
;                        (else 8)
;                        (#t #f))))
; (test 'second 'cond (cond ((< 1 2) (cons 1 2) 'second)))
; (test 'second-again 'cond (cond ((> 1 2) 'ok) (else (cons 1 2) 'second-again)))
; (test 1 'cond (cond (1)))
; (test 1 'cond (cond (#f) (1)))
; (test 1 'cond (cond (#f 7) (1)))
; (test 2 'cond (cond (#f 7) (1 => add1)))
; (test add1 'cond (let ([=> 9]) (cond (#f 7) (1 => add1))))

(defmacro incorrect-cond+
  ;; This is necessary to get around macroexpand raising a compiler exception.
  ;; I could write a `compiler-thrown?` macro, but that's a lot of work lol
  [& clauses]
  `(apply #'cond+ nil nil [~clauses]))

(deftest cond+-test
  (testing "returns nil when given no input"
    (is (nil? (cond+))))
  (testing "returns nil when clauses are empty or any test is nil (before a true test is found)"
    (is (nil? (cond+ nil)))
    (is (nil? (cond+ [])))
    (is (nil? (cond+ [nil])))
    (is (nil? (cond+ nil [true true])))
    (is (nil? (cond+ [false true] nil))))
  (testing "returns test if no expr given"
    (is (= '(1 2 3) (cond+ [(seq [1 2 3])])))
    (is (nil? (cond+ [(seq [])])))
    )
  (testing "returns first expr when associated test is true"
    (is (= :first (cond+ [true :first])))
    (is (= :first (cond+
                   [true :first]
                   [true :second]))))
  (testing "returns nil when all tests are false"
    (is (nil? (cond+ [false :first]))))
  (testing "returns expr when non-inital test is true"
    (is (= :second
           (cond+
            [false :first]
            [true :second]))))
  (testing "returns expr when non-inital test is true"
    (is (= :equal
           (cond+
            [(< 3 3) :greater]
            [(> 3 3) :less]
            [(= 3 3) :equal]))))
  (testing "don't run exprs of non-truthy branches"
    (is (let [view (atom 0)]
          (cond+ [false (swap! view inc)]
                 [true :first])
          (zero? @view))))
  (testing "runs exprs of first truthy branch"
    (let [view (atom 0)]
      (is (= 1 (cond+ [true (swap! view inc)]
                      [true :first])))
      (is (= 1 @view))))
  (testing "exprs have an implicit do"
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
  (testing ":else"
    (testing "always returns expr"
      (is (= :first (cond+ [:else :first]))))
    (testing "returns expr when other branches are falsey"
      (is (= :equal
             (cond+
              [(< 3 3) :greater]
              [(> 3 3) :less]
              [:else :equal]))))
    (testing "exprs have an implicit do"
      (is (= :second (cond+ [:else :first :second])))
      (is (= :second
             (cond+
              [(< 3 3) :greater]
              [(> 3 3) :less]
              [:else :equal :second])))
      (let [view (atom 0)]
        (is (= :second
               (cond+
                [(> 1 2) :ok]
                [:else (swap! view inc) :second])))
        (is (= 1 @view))))
    (testing "requires an expr"
      (is (thrown? IllegalArgumentException
                   (incorrect-cond+
                    [false :first]
                    [:else]))))
    (testing "can only be used in the last position"
      (is (thrown? IllegalArgumentException
                   (incorrect-cond+
                    [:else :first]
                    [true :second])))))
  (testing ":>"
    (testing "calls given function on result of test"
      (is (= 1 (cond+ [[1 2 3] :> first])))
      (is (= 2 (cond+ [(next [1 2 3]) :> first])))
      (is (= [-2 -3] (cond+ [(next [1 2 3]) :> (fn [x] (map - x))]))))
    (testing "requires a function in the final position"
      (is (thrown? IllegalArgumentException
                   (incorrect-cond+
                     [[1 2 3] :>])))
      (is (thrown? IllegalArgumentException
                   (incorrect-cond+
                     [[1 2 3] :> 1]))))
    )
  )
