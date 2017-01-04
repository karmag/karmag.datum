(ns karmag.datum.traversal-test
  (:require [clojure.test :refer :all]
            [karmag.datum.traversal :refer [walk walk-pre walk-post]]))

(def logger (partial walk
                     {:log []}
                     (fn [state item]
                       [(update-in state [:log] conj [:-> item]) item])
                     (fn [state item]
                       [(update-in state [:log] conj [:<- item]) item])))

(defrecord Test [a b])

(deftest simple-walk-test
  (testing "record"
    (let [[state data] (logger (Test. 1 2))]
      (is (= data (Test. 1 2)))
      (is (= state
             {:log [[:-> (Test. 1 2)]
                    [:-> 1]  [:<- 1]
                    [:-> 2]  [:<- 2]
                    [:<- (Test. 1 2)]]}))))
  (testing "map"
    (let [[state data] (logger {:a 1 :b 2})]
      (is (= data {:a 1 :b 2}))
      (is (= state
             {:log [[:-> {:a 1, :b 2}]
                    [:-> :a] [:<- :a]
                    [:-> 1]  [:<- 1]
                    [:-> :b] [:<- :b]
                    [:-> 2]  [:<- 2]
                    [:<- {:a 1, :b 2}]]}))))
  (testing "vector"
    (let [[state data] (logger [1 2])]
      (is (= data [1 2]))
      (is (vector? data))
      (is (= state
             {:log [[:-> [1 2]]
                    [:-> 1] [:<- 1]
                    [:-> 2] [:<- 2]
                    [:<- [1 2]]]}))))
  (testing "list"
    (let [[state data] (logger '(1 2))]
      (is (= data [1 2]))
      (is (list? data))
      (is (= state
             {:log [[:-> '(1 2)]
                    [:-> 2] [:<- 2]
                    [:-> 1] [:<- 1]
                    [:<- '(1 2)]]}))))
  (testing "set"
    (let [[state data] (logger #{1 2})]
      (is (= data #{1 2}))
      (is (set? data))
      (is (= state
             {:log [[:-> #{1 2}]
                    [:-> 1] [:<- 1]
                    [:-> 2] [:<- 2]
                    [:<- #{1 2}]]}))))
  (testing "object"
    (let [[state data] (logger :keyword)]
      (is (= data :keyword))
      (is (keyword? data))
      (is (= state
             {:log [[:-> :keyword] [:<- :keyword]]})))))

(deftest nested-test
  (let [[state data] (logger [{:a 1} '(10) [55] #{999}])]
    (is (= data [{:a 1} '(10) [55] #{999}]))
    (is (= state
           {:log [[:-> [{:a 1} '(10) [55] #{999}]]
                  [:-> {:a 1}] [:-> :a] [:<- :a] [:-> 1] [:<- 1] [:<- {:a 1}]
                  [:-> '(10)] [:-> 10] [:<- 10] [:<- '(10)]
                  [:-> [55]] [:-> 55] [:<- 55] [:<- [55]]
                  [:-> #{999}] [:-> 999] [:<- 999] [:<- #{999}]
                  [:<- [{:a 1} '(10) [55] #{999}]]]}))))

(deftest replacement-test
  (let [pre-fn (fn [state item]
                 (if-let [translated (get state item)]
                   [(update-in state [:count] inc) translated]
                   [state item]))
        test-data [:a {:a :a} '(:a) #{:a}]
        test-data-result [:alpha {:alpha :alpha} '(:alpha) #{:alpha}]]
    (testing "pre"
      (let [[state data] (walk-pre {:a :alpha, :count 0}
                                   pre-fn
                                   test-data)]
        (is (= (:count state) 5))
        (is (= data test-data-result))))
    (testing "post"
      (let [[state data] (walk-post {:a :alpha, :count 0}
                                    pre-fn
                                    test-data)]
        (is (= (:count state) 5))
        (is (= data test-data-result))))
    (testing "recursive replacement"
      (let [[state data] (walk-post {:b :beta, {0 {1 :beta}} :ok, :count 0}
                                    pre-fn
                                    {0 {1 :b}})]
        (is (= :ok data))))))
