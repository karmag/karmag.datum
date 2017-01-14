(ns karmag.datum.core-test
  (:require [clojure.test :refer :all]
            [karmag.datum.core :as d]))

(defn- fetch [string]
  (let [[result report] (d/process-string string)]
    (when-not (empty? report)
      (throw (ex-info "Report not empty"
                      {:report report})))
    result))

(deftest read-test
  (are [str data] (= (d/read (d/from-string str)) data)
    ;; define
    "#def [:id-array 10]"                         [(d/->Def :id-array 10 nil)]
    "#def [:id-array 15 {:a 1}]"                  [(d/->Def :id-array 15 {:a 1})]
    "#def {:id :id-map, :value 10}"               [(d/->Def :id-map 10 nil)]
    "#def {:id :id-map, :value 20, :default 100}" [(d/->Def :id-map 20 100)]
    ;; reference
    "#ref :id"                 [(d/->Ref :id nil)]
    "#ref [:id {:args 1}]"     [(d/->Ref :id {:args 1})]
    "#ref {:id 101, :args 22}" [(d/->Ref 101 22)]
    ;; arg
    "#arg :key" [(d/->Arg :key)]
    ;; code
    "#code (f args)" [(d/->Code '(f args))])
  (is (= [{} nil 10 (d/->Code '(f args))]
         (d/read (d/from-string "{} nil 10 #code (f args)")))))

(deftest def-ref-test
  (testing "basic expansion"
    (is (= (fetch "#def [:a 100] #ref :a") [100])))
  (testing "ref used in def"
    (is (= (fetch "#def [:a :val] #def [:b {:key #ref :a}] #ref :b")
           [{:key :val}]))))

(deftest def-function-test
  (testing "definition with default arg"
    (is (= (fetch "#def [:a {:key #arg :x} {:x 1}] #ref :a")
           [{:key 1}])))
  (testing "definition with user arg"
    (is (= (fetch "#def [:a {:key #arg :x}] #ref [:a {:x 10}]")
           [{:key 10}])))
  (testing "definition with default/user args"
    (is (= (fetch "#def [:a {:key #arg :x, :kai #arg :y} {:x 99}]
                   #ref [:a {:y 44}]")
           [{:key 99, :kai 44}])))
  (testing "nested definitions"
    (is (= (fetch "#def [:a {:key #arg :x}]
                   #def [:b {:bee #arg :y}]
                   #ref [:a {:x #ref [:b {:y 22}]}]")
           [{:key {:bee 22}}]))))

(deftest error-report-test
  (let [get-rep #(-> % d/from-string d/read d/build :report)
        is-msg? (fn [report txt]
                  (is (some #(.contains % txt) (map :message report))))]
    (testing "definitions"
      (is-msg? (get-rep "[#def [:a 1]]")
               "Definition in non-root position")
      (is-msg? (get-rep "[#def [:a 1] #def [:a 2]]")
               "Duplicate definition"))
    (testing "definition args"
      (is-msg? (get-rep "#def [:a [#arg :x]] #ref :a")
               "No replacement defined for (:x)")
      (is-msg? (get-rep "#def [:a [#arg :x]] #ref [:a {:x 1, :y 2}]")
               "Extraneous arguments (:y)"))))

;; TODO read context, data that follows through to the error-reporting
;; / extraction part.

(deftest code-test
  (is (= (fetch "#code (map (partial + 10) (range 5))")
         [[10 11 12 13 14]]))
  (is (= (fetch "#code (+ 100 #ref :a) #def [:a 7]")
         [107]))
  (is (= (fetch "#def [:a #code (/ 1 2)] #ref :a")
         [1/2]))
  (is (= (fetch "#code(->> (range 5) (filter odd?))")
         [[1 3]]))
  (is (= (fetch "#code(+ 5 10 #code(int \\a))")
         [112])))

(deftest configuration-test
  (is (= (d/process-string "#def [:a #code(+ 1 #arg :i)] #ref [:a {:i 10}]")
         (d/process-string
          ["#karmag.datum/def [:a #karmag.datum/code(+ 1 #karmag.datum/arg :i)]"
           "#karmag.datum/ref [:a {:i 10}]"]
          d/namespace-config)
         [[11] []]))
  (testing "default tag handler"
    (is (= (d/process-string "#tag-a #tag-b item"
                             (assoc d/default-config :default
                                    #(hash-map :tag %1 :item %2)))
           '[[{:tag tag-a
                :item {:tag tag-b
                       :item item}}]
             []])))
  (is (= (fetch "#tag-a #tag-b item")
         '[[tag-a [tag-b item]]])))
