# cond-plus

An adaption of the Racket `cond` macro for Clojure.

Details can be found in the [docs](doc/intro.md).

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
