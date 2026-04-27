# com.noahbogart/cond-plus

[![Clojars Project](https://img.shields.io/clojars/v/com.noahbogart/cond-plus.svg)](https://clojars.org/com.noahbogart/cond-plus)
[![cljdoc badge](https://cljdoc.org/badge/com.noahbogart/cond-plus)](https://cljdoc.org/d/com.noahbogart/cond-plus)

An adaption of the Racket `cond` macro for Clojure.

Details can be found in the [docs](doc/intro.md).

## Example

```clojure
(require '[com.noahbogart.cond-plus :refer [cond+]])

(cond+)
=> nil

(cond+
  [false 10]
  [:else 5])
=> 5

(let [i -5]
  (cond+
    [(pos? i) "doesn't get here"]
    [(zero? i) "doesn't get here, either"]
    [(neg? i) :here]))
=> :here

(cond+
  [(next [1 2 3]) :> (fn [x] (map - x))])
=> [-2 -3]

(cond+
  [(next [1 2 3])])
=> [2 3]
```

## License

Copyright © Noah Bogart

Distributed under the Mozilla Public License version 2.0.
