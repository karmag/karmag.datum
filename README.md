# Datum

This project uses [semantic versioning](http://semver.org/).

## Usage

The basics of datum revolves around the *process* commands. They
all take the form of *process-x* => `[result, report]`. In this
documentation results and reports are expanded separately for
readability. If there are no reports reports are not shown.

The two basic form are `process` and `process-string` which both
take a single value or a sequence of values. `process` interprets
argument types in the same type as
`clojure.java.io/reader`. `process-string` will treat arguments as
string data and wrap them in readers. If individual wrapping is
needed `from-string` can be used.

### Data substitution

`#def` and `#ref` are the basic building blocks for substitution.

    (process-string "#def [:x 5], #ref :x")
    ;; => (5)

`#def` on the root level are removed from the output and turned
into nil if nested deeper. Non root defs generate warnings.

    (process-string "#def [:x 5] [#def [:y 10]]")
    ;; => ([{:id :y, :value 10, :default nil}])

    ;; report =>
    ;; [{:message "Definition in non-root position",
    ;;   :severity :warn,
    ;;   :data "<additional data>"}]

Both *def* and *ref* accept an extra argument that handles
arguments. If given for *def* it will be used as a default argument
if non is given. If given for *ref* it will use that as the
substitution value. If both *ref* argument and default argument is
missing a warning is generated and the value becomes `nil`.

    (process-string "#def [:x [hello #arg :item]], #ref [:x {:item world}]")
    ;; => ([hello world])

    (process-string "#def [:x [hello #arg :item] {:item you}], #ref :x")
    ;; => ([hello you])

    (process-string "#def [:x [hello #arg :item]], #ref :x")
    ;; => ([hello nil])

    ;; report =>
    ;; [{:message "No replacement defined for (:item)",
    ;;   :severity :warn,
    ;;   :data "<additional data>"}]

### Code

A `#code` tag is supported for arbitrary transformations. Symbols
have to be whitelisted for code tag to work. By default pure
functions and macros from the `clojure.core` namespace
are whitelisted.

    (process-string "#code(+ 15 (/ 1 2))")
    ;; => (31/2)

Trying to execute non whitelisted code generates an error.

    (process-string "#code(defrecord Alpha [a b c])")
    ;; => ((let*
    ;; =>   []
    ;; =>   nil
    ;; =>   nil
    ;; =>   (deftype*
    ;; =>    karmag.datum.document-generator/Alpha
    ;; =>    karmag.datum.document_generator.Alpha
    ;; =>    [a b c __meta __extmap]
    ;; =>    :implements
    ;; =>    [clojure.lang.IRecord
    ;; =>     clojure.lang.IHashEq
    ;; =>     clojure.lang.IObj
    ;; =>     clojure.lang.ILookup
    ;; =>     clojure.lang.IKeywordLookup
    ;; =>     clojure.lang.IPersistentMap
    ;; =>     java.util.Map
    ;; =>     java.io.Serializable]
    ;; =>    nil
    ;; =>    nil
    ;; =>    nil
    ;; =>    nil
    ;; =>    nil
    ;; =>    nil
    ;; =>    nil
    ;; =>    k__6454__auto__
    ;; =>    nil
    ;; =>    nil
    ;; =>    nil
    ;; =>    (if k__6448__auto__ k__6448__auto__ (new Alpha a b c __meta nil))
    ;; =>    (clojure.core/condp
    ;; =>     clojure.core/identical?
    ;; =>     k__6446__auto__
    ;; =>     :a
    ;; =>     (new Alpha G__609 b c __meta __extmap)
    ;; =>     :b
    ;; =>     (new Alpha a G__609 c __meta __extmap)
    ;; =>     :c
    ;; =>     (new Alpha a b G__609 __meta __extmap)
    ;; =>     (new
    ;; =>      Alpha
    ;; =>      a
    ;; =>      b
    ;; =>      c
    ;; =>      __meta
    ;; =>      (clojure.core/assoc __extmap k__6446__auto__ G__609)))
    ;; =>    (clojure.lang.RecordIterator. G__609 [:a :b :c] nil)
    ;; =>    nil
    ;; =>    v__6442__auto__
    ;; =>    nil
    ;; =>    nil
    ;; =>    (nil this__6435__auto__ e__6436__auto__)
    ;; =>    nil
    ;; =>    nil
    ;; =>    (clojure.core/case
    ;; =>     k__6432__auto__
    ;; =>     :a
    ;; =>     (if gclass -a thunk)
    ;; =>     :b
    ;; =>     (if gclass -b thunk)
    ;; =>     :c
    ;; =>     (if gclass -c thunk)
    ;; =>     nil)
    ;; =>    (clojure.core/case
    ;; =>     k__6429__auto__
    ;; =>     :a
    ;; =>     a
    ;; =>     :b
    ;; =>     b
    ;; =>     :c
    ;; =>     c
    ;; =>     (clojure.core/get __extmap k__6429__auto__ else__6430__auto__))
    ;; =>    (.valAt this__6426__auto__ k__6427__auto__ nil)
    ;; =>    (new Alpha a b c G__609 __extmap)
    ;; =>    __meta
    ;; =>    G__609
    ;; =>    nil
    ;; =>    nil)
    ;; =>   nil
    ;; =>   (clojure.core/defn
    ;; =>    ->Alpha
    ;; =>    "Positional factory function for class karmag.datum.document_generator.Alpha."
    ;; =>    [a b c]
    ;; =>    (new karmag.datum.document_generator.Alpha a b c))
    ;; =>   (clojure.core/defn
    ;; =>    map->Alpha
    ;; =>    "Factory function for class karmag.datum.document_generator.Alpha, taking a map of keywords to field values."
    ;; =>    ([m__6522__auto__] nil))
    ;; =>   karmag.datum.document_generator.Alpha))

    ;; report =>
    ;; [{:message "Code expansion failed",
    ;;   :severity :error,
    ;;   :data "<additional data>"}]

## Developer

This documentation is generated from the `document_generator.clj`
file under `test`, it should not be modified manually. It is
regenerated by running `lein test`.

