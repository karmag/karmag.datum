# Datum

_A single place for your data_

This project uses [semantic versioning](http://semver.org/).

## Usage

The basics of datum revolves around the `process` commands. They
all take the form of `process... => [result, report]`. In this
documentation results and reports are expanded separately for
readability. If there are no reports reports are not shown.

The two basic form are `process` and `process-string` which both
take a single value or a sequence of values. `process` interprets
argument types in the same type as
`clojure.java.io/reader`. `process-string` will treat arguments as
string data and wrap them in readers. If individual wrapping is
needed `from-string` can be used.

    (require '[karmag.datum.core
               :refer [process process-string from-string]])

    (process file)                        => [result, report]
    (process [url, (from-string "edn")])  => [result, report]
    (process-string "[1 2 3]")            => [result, report]
    (process-string ["[1 2 3]", "hello"]) => [result, report]

An optional second argument may be given that specifies additional
configuration options. Two basic configuration are
supplied. `default-config` and `namespace-config`. The default
config uses non-namespaced tags while the namespaced uses tags in
the karmag.datum namespace.

    (require '[karmag.datum.core
               :refer [process-string default-config namespace-config]])

    (process-string "#code(+ 1 2)" default-config)
    (process-string "#karmag.datum/code(+ 1 2)" namespace-config)

The configurations are maps where the keys :readers and :default
are interpreted as the corresponding key in a `clojure.edn/read`
call. The default :default is `vector`.

    (process-string "#tag #nested data")
    ;; => [[tag [nested data]]]

### Data substitution

`#def` and `#ref` are the basic building blocks for substitution.

    (process-string "#def [:x 5], #ref :x")
    ;; => [5]

`#def` on the root level are removed from the output and turned
into nil if nested deeper. Non root defs generate warnings.

    (process-string "#def [:x 5] [#def [:y 10]]")
    ;; => [[nil]]

    ;; errors =>
    ;; [{:message "Definition in non-root position",
    ;;   :severity :warn,
    ;;   :data <additional-data>}]

Both `def` and `ref` accept an extra argument that handles
arguments. If given for `def` it will be used as a default argument
if non is given. If given for `ref` it will use that as the
substitution value. If both `ref` argument and default argument is
missing a warning is generated and the value becomes `nil`.

    (process-string "#def [:x [hello #arg :item]], #ref [:x {:item world}]")
    ;; => [[hello world]]

    (process-string "#def [:x [hello #arg :item] {:item you}], #ref :x")
    ;; => [[hello you]]

    (process-string "#def [:x [hello #arg :item]], #ref :x")
    ;; => [[hello nil]]

    ;; errors =>
    ;; [{:message "No replacement defined for (:item)",
    ;;   :severity :warn,
    ;;   :data <additional-data>}]

### Code

A `#code` tag is supported for arbitrary transformations. Symbols
have to be whitelisted for code tag to work. By default pure
functions and macros from the `clojure.core` namespace
are whitelisted.

    (process-string "#code(+ 15 (/ 1 2))")
    ;; => [31/2]

Trying to execute non whitelisted code generates an error.

    (process-string "#code(defrecord Alpha [a b c])")
    ;; => [:karmag.datum.error/unresolved-code]

    ;; errors =>
    ;; [{:message "Code expansion failed",
    ;;   :severity :error,
    ;;   :data <additional-data>}]

