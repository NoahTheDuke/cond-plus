# cond-plus

An adaption of the Racket `cond` macro for Clojure.

## Usage

The below takes inspiration from the [Racket docs][1]:

[1]: https://docs.racket-lang.org/reference/if.html?q=cond#%28form._%28%28lib._racket%2Fprivate%2Fletstx-scheme..rkt%29._cond%29%29

(*cond+* & _cond-clause_)

```
cond-clause = [test-expr & body]
            | [test-expr :> fn-expr]
            | [test-expr]
            | [:else & body]
```

Each `test-expr` is evaluated one at a time. If the `test-expr` returns logical
true (or is `:else`), the form is evaluated as described below and no further
`cond-clause`s are evaluated. `(cond+)` returns `nil`.

### `[test-expr & body]`
Evaluates `body` in an implicit `do`.

### `[test-expr :> fn-expr]`
`fn-expr` must be a function that accepts one argument. The result of the
`test-expr` is passed to the `fn-expr` and that result is returned.

`=>` is also accepted for historical reasons.

### `[test-expr]`
The result of the `test-expr` is returned.

### `[:else & body]`
If an `[:else & body]` is present, it must be the last `cond-clause` and
it must be the only `:else` clause. Evaluates `body` in an implicit `do`.

`else` is also accepted for historical reasons.

```clojure
> (cond+)
nil
> (cond+
   [:else 5])
5
> (cond+
   [(pos? -5) "doesn't get here"]
   [(zero? -5) "doesn't get here, either"]
   [(neg? 5) :here])
:here
> (cond+
   [(next [1 2 3]) :> (fn [x] (map - x))])
[-2 -3]
> (cond+
   [(next [1 2 3])])
[2 3]
```


## License

Copyright © 2020 Noah Bogart

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
