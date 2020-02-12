# cond-plus

An adaption of the Racket `cond` macro, updated for Clojure.

## Usage

The below is adapted from the [Racket docs](https://docs.racket-lang.org/reference/if.html?q=cond#%28form._%28%28lib._racket%2Fprivate%2Fletstx-scheme..rkt%29._cond%29%29):

(*cond+* _cond-clause_ ...)

```
cond-clause = [test-expr then-body ...+]
            | [:else then-body ...+]
            | [test-expr :> proc-expr]
            | [test-expr]
```

A `cond-clause` that starts with `:else` must be the last
`cond-clause`.

If no `cond-clause`s are present, the result is `nil`.

If only a `[else then-body ...+]` is present, then the
`then-body`s are evaluated. The results from all but the last
`then-body` are ignored. The results of the last
`then-body`, which is in tail position with respect to the
`cond+` form, are the results for the whole `cond+`
form.

Otherwise, the first `test-expr` is evaluated. If it produces
`#f`, then the result is the same as a `cond+` form with
the remaining `cond-clause`s, in tail position with respect to
the original `cond+` form. Otherwise, evaluation depends on the
form of the `cond-clause`:

### `[test-expr then-body ...+]`
The `then-body`s are
evaluated in order, and the results from all but the last
`then-body` are ignored. The results of the last
`then-body`, which is in tail position with respect to the
`cond+` form, provides the result for the whole `cond+`
form.

### `[test-expr => proc-expr]`
The `proc-expr` is
evaluated, and it must produce a procedure that accepts one argument,
otherwise it will raise an exception. The procedure is applied
to the result of `test-expr` in tail position with respect to
the `cond+` expression.

`[test-expr]`
The result of the `test-expr` is
returned as the result of the `cond+` form. The
`test-expr` is not in tail position.

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

### :else

Recognized specially within forms like `cond+`. An
`:else` form as an expression is a syntax error.


### :>

Recognized specially within forms like `cond+`. A
`:>` form as an expression is a syntax error.


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
