(ns cond-plus.core)

(defmacro cond+
  [& clauses]
  (letfn
   [(cond-loop
      [tests]
      (let [line (first tests)
            others (next tests)]
        (when (some? line)
          (if-not (or (list? line)
                      (vector? line))
            (throw (IllegalArgumentException. "clause is not in a vector"))
            (let [test (first line)
                  value (next line)]
              (if (= 'else test)
                (if (seq others)
                  (throw (IllegalArgumentException. ":else not last"))
                  (if (nil? value)
                    (throw (IllegalArgumentException. "missing expression in else clause"))
                    `(let [] ~@value)))
                (if (= '=> (first value))
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
    (cond-loop clauses)))
