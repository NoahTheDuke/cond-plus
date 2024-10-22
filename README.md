# cond-plus

An adaption of the Racket `cond` macro for Clojure.

Details can be found in the [docs](doc/intro.md).

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


## License

Copyright © Noah Bogart

Distributed under the Mozilla Public License version 2.0.
