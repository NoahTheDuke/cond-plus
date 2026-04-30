default:
    @just --list

repl arg="":
    clojure -M:dev:test:v1.12{{arg}}:repl

clojure-lsp:
    clojure-lsp diagnostics

alias lint := clojure-lsp

[no-exit-message]
@test-clj-raw *args:
    clojure -M:test:runner --md README.md {{args}}

[no-exit-message]
test-clj *args="--output summary":
    just clojure-lsp
    just test-clj-raw {{args}}

[no-exit-message]
test-cljs:
    npx shadow-cljs compile node-test
    node target/shadow-node-test/node-tests.js

[no-exit-message]
test-all *args="--output summary":
    just clojure-lsp
    clojure -M:v1.10:test:runner --md README.md {{args}}
    clojure -M:v1.11:test:runner --md README.md {{args}}
    clojure -M:v1.12:test:runner --md README.md {{args}}
    just test-cljs

@clojars:
    env CLOJARS_USERNAME='noahtheduke' CLOJARS_PASSWORD=`cat ../clojars.txt` clojure -M:clein deploy
