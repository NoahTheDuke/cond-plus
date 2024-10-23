# Introduction to cond-plus

The below takes inspiration from the [Racket docs][1]:

[1]: https://docs.racket-lang.org/reference/if.html?q=cond#%28form._%28%28lib._racket%2Fprivate%2Fletstx-scheme..rkt%29._cond%29%29


```
(cond+ & clause)

clause = [test-expr & body]
       | [test-expr :> fn-expr]
       | [test-expr]
       | [:else & body]
```

Takes any number of clauses, which must be lists or vectors. The `test-expr` of the first clause is evaluated. If it's a logical true value (or the bare symbol `else`), then the rest of the clause is evaluated as described below and the resulting value is returned. Otherwise, repeat these steps for the next clauses. `(cond+)` returns `nil`.

If a `clause` is not a vector or list, an `IllegalArgumentError` is thrown.

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

If an `:else` is not the last `clause`, an `IllegalArgumentError` is thrown.  If a `body` is not included, an `IllegalArgumentError` is thrown.

`else` can be used instead of `:else`.

```clojure
(cond+)
=> nil

(cond+
 [:else 5])
=> 5

(cond+
 [(pos? -5) "doesn't get here"]
 [(zero? -5) "doesn't get here, either"]
 [(neg? -5) :here])
=> :here

(cond+
 [(next [1 2 3]) :> (fn [x] (map - x))])
=> [-2 -3]

(cond+
 [(next [1 2 3])])
=> [2 3]
```
