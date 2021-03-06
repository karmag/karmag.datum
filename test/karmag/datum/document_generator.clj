(ns karmag.datum.document-generator
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer :all]
            [clojure.test :refer :all]
            [karmag.datum.core :refer [process-string]]))

(defn- make-example [data expected]
  (let [[result report] (process-string data)]
    (when expected
      (assert (= result expected) (str "Expected " expected " but was " result)))
    {:code (list 'process-string data)
     :result result
     :report (when-not (empty? report)
               report)}))

;; mission statement - intended for configuration use, can be misused
;; if used to parse user-input.

(def ^:private doc-parts
  ["# Datum"

   "_A single place for your data_"

   "This project uses [semantic versioning](http://semver.org/)."

   "## Usage"

   "The basics of datum revolves around the `process` commands. They
   all take the form of `process... => [result, report]`. In this
   documentation results and reports are expanded separately for
   readability. If there are no reports reports are not shown."

   "The two basic form are `process` and `process-string` which both
   take a single value or a sequence of values. `process` interprets
   argument types in the same type as
   `clojure.java.io/reader`. `process-string` will treat arguments as
   string data and wrap them in readers. If individual wrapping is
   needed `from-string` can be used."

   [:code ["(require '[karmag.datum.core"
           "           :refer [process process-string from-string]])"
           ""
           "(process file)                        => [result, report]"
           "(process [url, (from-string \"edn\")])  => [result, report]"
           "(process-string \"[1 2 3]\")            => [result, report]"
           "(process-string [\"[1 2 3]\", \"hello\"]) => [result, report]"]]

   "An optional second argument may be given that specifies additional
   configuration options. Two basic configuration are
   supplied. `default-config` and `namespace-config`. The default
   config uses non-namespaced tags while the namespaced uses tags in
   the karmag.datum namespace."

   [:code ["(require '[karmag.datum.core"
           "           :refer [process-string default-config namespace-config]])"
           ""
           "(process-string \"#code(+ 1 2)\" default-config)"
           "(process-string \"#karmag.datum/code(+ 1 2)\" namespace-config)"]]

   "The configurations are maps where the keys :readers and :default
   are interpreted as the corresponding key in a `clojure.edn/read`
   call. The default :default is `vector`."

   [:example (make-example "#tag #nested data"
                           '[[tag [nested data]]])]

   "### Data substitution"

   "`#def` and `#ref` are the basic building blocks for substitution."
   [:example (make-example "#def [:x 5], #ref :x"
                           [5])]

   "`#def` on the root level are removed from the output and turned
   into nil if nested deeper. Non root defs generate warnings."
   [:example (make-example "#def [:x 5] [#def [:y 10]]"
                           [[nil]])]

   "Both `def` and `ref` accept an extra argument that handles
   arguments. If given for `def` it will be used as a default argument
   if non is given. If given for `ref` it will use that as the
   substitution value. If both `ref` argument and default argument is
   missing a warning is generated and the value becomes `nil`."
   [:example (make-example "#def [:x [hello #arg :item]], #ref [:x {:item world}]"
                           '[[hello world]])]

   [:example (make-example "#def [:x [hello #arg :item] {:item you}], #ref :x"
                           '[[hello you]])]

   [:example (make-example "#def [:x [hello #arg :item]], #ref :x"
                           '[[hello nil]])]

   "### Code"

   "A `#code` tag is supported for arbitrary transformations. Symbols
   have to be whitelisted for code tag to work. By default pure
   functions and macros from the `clojure.core` namespace
   are whitelisted."
   [:example (make-example "#code(+ 15 (/ 1 2))"
                           [31/2])]

   "Trying to execute non whitelisted code generates an error."
   [:example (make-example "#code(defrecord Alpha [a b c])"
                           [:karmag.datum.error/unresolved-code])]

   ;; TODO whitelisting guide
   ])

(defn- lines [x] (.split x "\n"))
(defn- unlines [xs] (->> xs (interpose "\n") (apply str)))
(defn- indent [ind s] (->> s lines (map str (repeat ind)) unlines))

(defn- render-example [{:keys [code result report]}]
  (->> [(indent "    "
                (with-out-str
                  (binding [*print-pprint-dispatch* code-dispatch]
                    (pprint code))))
        (indent "    ;; => "
                (with-out-str
                  (pprint result)))
        (when report
          (unlines [""
                    "    ;; errors =>"
                    (indent
                     "    ;; "
                     (with-out-str
                       (pprint (mapv #(assoc-in % [:data] '<additional-data>)
                                     report))))]))]
       (remove nil?)
       unlines))

(deftest generate-documentation
  (with-open [writer (io/writer "README.md")]
    (doseq [part doc-parts]
      (cond
        ;; text
        (string? part)
        (do (.write writer (->> part lines (map #(.trim %)) unlines))
            (.write writer "\n\n"))
        ;; example
        (and (vector? part) (-> part first (= :example)))
        (do (.write writer (render-example (second part)))
            (.write writer "\n\n"))
        ;; code
        (and (vector? part) (-> part first (= :code)))
        (do (doseq [line (second part)]
              (if (empty? line)
                (.write writer "\n")
                (do (.write writer (indent "    " line))
                    (.write writer "\n"))))
            (.write writer "\n"))
        ;; error
        :else
        (throw (ex-info (str "Unknown document part: " part)
                        {:part part}))))))
