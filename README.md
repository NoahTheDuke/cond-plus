# cond-plus

[![Clojars Project](https://img.shields.io/clojars/v/io.github.noahtheduke/cond-plus.svg)](https://clojars.org/io.github.noahtheduke/cond-plus)
[![cljdoc badge](https://cljdoc.org/badge/io.github.noahtheduke/cond-plus)](https://cljdoc.org/d/io.github.noahtheduke/cond-plus)

An adaption of the Racket `cond` macro for Clojure.

Details can be found in the [docs](doc/intro.md).

NOTE: with v1.3.0, the clojars group/artifact id is now `io.github.noahtheduke/cond-plus` and the primary namespace is now `noahtheduke.cond-plus`. Please update your dependencies accordingly.

## Example

```clojure
(require '[noahtheduke.cond-plus :refer [cond+]])

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
