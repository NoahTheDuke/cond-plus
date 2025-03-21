(ns cond-plus.core
  #?(:clj (:import
           (java.lang IllegalArgumentException))))

#_{:clj-kondo/ignore [:uninitialized-var]}
(def ^{:doc "Only useful to silence linter errors."} =>)

#_{:clj-kondo/ignore [:uninitialized-var]}
(def ^{:doc "Only useful to silence linter errors."} else)

(defmacro cond+
  "Takes any number of clauses, which must be lists or vectors. The `test-expr` of the first clause is evaluated. If it's a logical true value (or the bare symbol `else`), then the rest of the clause is evaluated as described below and the resulting value is returned. Otherwise, repeat these steps for the next clauses. `(cond+)` returns `nil`.

  ```
  clause = [test-expr & body]
         | [test-expr :> fn-expr]
         | [test-expr]
         | [:else & body]
  ```

  ### `[test-expr & body]`
  Evaluates `body` in an implicit `do`.

  ### `[test-expr :> fn-expr]`
  `fn-expr` must be a function that accepts one argument. The result of the `test-expr` is passed to the `fn-expr` and that result is returned.

  If an `fn-expr` is not provided or more than one form is included after the `:>`, an `IllegalArgumentError` is thrown.

  The bare symbol `=>` can be used instead of `:>`.

  ### `[test-expr]`
  The result of the `test-expr` is returned.

  ### `[:else & body]`
  Evaluates `body` in an implicit `do`.

  If an `:else` is not the last `cond-clause`, an `IllegalArgumentError` is
  thrown.  If a `body` is not included, an `IllegalArgumentError` is thrown.

  `else` can be used instead of `:else`."
  [& clauses]
  (letfn
    [(cond-loop
       [tests]
       ;; An empty call returns nil, which can happen in the top-level call (cond+) and
       ;; when recursing and there aren't any forms left to process
       (when (seq tests)
         (let [line (first tests)
               others (next tests)]
           ;; Only accept lines wrapped in () or []
           (if-not (or (list? line)
                       (vector? line))
             (throw (#?(:clj IllegalArgumentException. :cljs js/Error.) "clause is not in a vector"))
             ;; Because a line is a sequence, we'll step through the line form by form.
             ;; The first form is called text-expr, and then we call next on the rest
             ;; of the forms, and call the result value.
             ;; text-expr will be one of 2 possible forms:
             ;; 1. a form that is evaluated
             ;; 2. :else, which is treated as a truthy branch
             ;; value will be one of 3 possible forms:
             ;; 1. nil, which means there were no other forms in the line
             ;; 2. :> branch, which requires exactly 2 forms
             ;; 3. any number of forms, where the first is not :>
             (let [test-expr (first line)
                   value (next line)]
               ;; else should be in the final position
               (if (#{'else :else} test-expr)
                 ;; Else branch needs to be in final place
                 (if (seq others)
                   (throw (#?(:clj IllegalArgumentException. :cljs js/Error.) ":else not last"))
                   ;; Else branch can't be empty
                   (if (nil? value)
                     (throw (#?(:clj IllegalArgumentException. :cljs js/Error.) "missing expression in :else clause"))
                     `(let [result# (do ~@value)] result#)))
                 ;; Recurse into the rest of the lines before processing each branch,
                 ;; because we will be unrolling the results into the else positions of
                 ;; each branch
                 (let [exp (cond-loop others)]
                   ;; nil branch
                   (if (nil? value)
                     `(if-let [result# (do ~test-expr)]
                        result#
                        ~exp)
                     ;; :> needs to be in the first position of the value form to be
                     ;; properly handled
                     (if (#{'=> :>} (first value))
                       ;; :> branch needs exactly 1 additional form
                       (if (= 2 (count value))
                         `(if-let [result# (do ~test-expr)]
                            ;; Call the function with the result of the test-expr
                            (~(second value) result#)
                            ~exp)
                         (throw (#?(:clj IllegalArgumentException. :cljs js/Error.) "bad :> clause")))
                       ;; everything else, aka a test and then any number of forms
                       ;; wrapped in a do
                       `(if ~test-expr
                          (let [result# (do ~@value)] result#)
                          ~exp))))))))))]
    (cond-loop clauses)))
