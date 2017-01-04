(ns karmag.datum.whitelist)

(defn lookup [xs]
  (->> xs
       (map (fn [item]
              (let [sym (if (symbol? item) item (symbol item))
                    var (ns-resolve (symbol "clojure.core") sym)]
                (assert var (str "Can't resolve '" sym "' for default whitelist"))
                [sym @var])))
       (into {})))

(def pure-whitelist
  "Pure clojure.core functions"
  (lookup
   '[* *' + +' - -' / < <= = == > >= apply array-map assoc assoc-in
     associative? bigdec bigint biginteger bit-and bit-and-not bit-clear
     bit-flip bit-not bit-or bit-set bit-shift-left bit-shift-right
     bit-test bit-xor boolean butlast byte cat char char? class? coll?
     comp comparator compare complement completing concat conj cons
     constantly contains? count counted? cycle dec dec' decimal? dedupe
     denominator disj dissoc distinct distinct? double drop drop-last
     drop-while eduction empty empty? ensure-reduced even? every-pred
     every? false? ffirst filter filterv find first flatten float float?
     fn? fnext fnil format frequencies future? get get-in group-by hash
     hash-map hash-ordered-coll hash-set hash-unordered-coll identical?
     identity ifn? inc inc' instance? int integer? interleave interpose
     into isa? iterate juxt keep keep-indexed key keys keyword keyword?
     last list list* list? long map map-entry? map-indexed map? mapcat
     mapv max max-key merge merge-with min min-key mix-collection-hash mod
     name neg? next nfirst nil? nnext not not-any? not-empty not-every?
     not= nth nthnext nthrest num number? numerator odd? partial partition
     partition-all partition-by peek pop pos? primitives-classnames quot
     range ratio? rational? rationalize re-find re-groups re-matches
     re-pattern re-seq record? reduce reduce-kv reduced reduced? reductions
     rem remove repeat repeatedly replace rest reverse reversible? rseq
     rsubseq satisfies? second select-keys seq seq? sequence sequential?
     set set? short some some-fn some? sort sort-by sorted-map
     sorted-map-by sorted-set sorted-set-by sorted? special-symbol?
     split-at split-with str string? subs subseq subvec symbol symbol?
     tagged-literal tagged-literal? take take-last take-nth take-while
     trampoline transduce tree-seq true? unchecked-add unchecked-add-int
     unchecked-byte unchecked-char unchecked-dec unchecked-dec-int
     unchecked-divide-int unchecked-double unchecked-float unchecked-inc
     unchecked-inc-int unchecked-int unchecked-long unchecked-multiply
     unchecked-multiply-int unchecked-negate unchecked-negate-int
     unchecked-remainder-int unchecked-short unchecked-subtract
     unchecked-subtract-int unsigned-bit-shift-right update update-in val
     vals var? vary-meta vec vector vector-of vector? volatile? with-meta
     xml-seq zero? zipmap]))
