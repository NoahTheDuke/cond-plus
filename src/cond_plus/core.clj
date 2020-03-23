(ns cond-plus.core)

(defmacro cond+
  "Each test-expr is evaluated one at a time. If the test-expr returns logical
  true (or is :else), the form is evaluated as described below and no further
  cond-clauses are evaluated. (cond+) returns nil."
  [& body]
  (letfn
    [(cond-loop
       [tests]
       (when (seq tests)
         (let [line (first tests)
               others (next tests)]
           (if-not (or (list? line)
                       (vector? line))
             (throw (IllegalArgumentException. "clause is not in a vector"))
             (let [test (first line)
                   value (next line)]
               (if (or (= 'else test)
                       (= :else test))
                 (if (seq others)
                   (throw (IllegalArgumentException. ":else not last"))
                   (if (nil? value)
                     (throw (IllegalArgumentException. "missing expression in else clause"))
                     `(let [] ~@value)))
                 (if (or (= '=> (first value))
                         (= :> (first value)))
                   (if (= 2 (count value))
                     (let [exp (cond-loop others)
                           gen (gensym)]
                       `(let [~gen ~test]
                          (if ~gen
                            (~(second value) ~gen)
                            ~exp)))
                     (throw (IllegalArgumentException. "bad => clause")))
                   (let [exp (cond-loop others)]
                     (if (nil? value)
                       (let [gen (gensym)]
                         `(let [~gen ~test]
                            (if ~gen ~gen ~exp)))
                       `(if ~test
                          (let [] ~@value)
                          ~exp))))))))))]
    (cond-loop body)))
