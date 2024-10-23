(ns cond-plus.core
  (:import
    (java.lang IllegalArgumentException)))

#_{:clj-kondo/ignore [:uninitialized-var]}
(def ^{:doc "Only useful to silence linter errors."} =>)

#_{:clj-kondo/ignore [:uninitialized-var]}
(def ^{:doc "Only useful to silence linter errors."} else)

(defmacro cond+
  "Each test-expr is evaluated one at a time. If the test-expr returns logical
  true (or is :else), the form is evaluated as described below and no further
  cond-clauses are evaluated. (cond+) returns nil."
  [& body]
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
             (throw (IllegalArgumentException. "clause is not in a vector"))
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
                   (throw (IllegalArgumentException. ":else not last"))
                   ;; Else branch can't be empty
                   (if (nil? value)
                     (throw (IllegalArgumentException. "missing expression in :else clause"))
                     `(do ~@value)))
                 ;; Recurse into the rest of the lines before processing each branch,
                 ;; because we will be unrolling the results into the else positions of
                 ;; each branch
                 (let [exp (cond-loop others)
                       gen (gensym)]
                   ;; nil branch
                   (if (nil? value)
                     `(let [~gen ~test-expr]
                        (if ~gen ~gen ~exp))
                     ;; :> needs to be in the first position of the value form to be
                     ;; properly handled
                     (if (#{'=> :>} (first value))
                       ;; :> branch needs exactly 1 additional form
                       (if (= 2 (count value))
                         `(let [~gen ~test-expr]
                            (if ~gen
                              ;; Call the function with the result of the test-expr
                              (~(second value) ~gen)
                              ~exp))
                         (throw (IllegalArgumentException. "bad :> clause")))
                       ;; everything else, aka a test and then any number of forms
                       ;; wrapped in a do
                       `(if ~test-expr
                          (do ~@value)
                          ~exp))))))))))]
    (cond-loop body)))
