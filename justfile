default:
    @just --list

repl arg="":
    clojure -M:dev:test:v1.12{{arg}}:repl

clojure-lsp:
    clojure-lsp diagnostics

[no-exit-message]
@test-raw *args:
    clojure -M:test:runner --md README.md {{args}}

[no-exit-message]
test *args="--output summary":
    just clojure-lsp
    just test-raw {{args}}

[no-exit-message]
test-all *args="--output summary":
    just clojure-lsp
    clojure -M:v1.10:test:runner --md README.md {{args}}
    clojure -M:v1.11:test:runner --md README.md {{args}}
    clojure -M:v1.12:test:runner --md README.md {{args}}
