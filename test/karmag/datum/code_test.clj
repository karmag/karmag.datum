(ns karmag.datum.code-test
  (:require [clojure.test :refer :all]
            [karmag.datum.code :as c]))

(defn- success [form]
  (let [result (c/resolve form)]
    (assert (empty? (:report result))
            (str "Report not empty: " (into [] (:report result))))
    (:result result)))

(defn- failure [form & check-parts]
  (let [result (c/resolve form)
        errors (map :message (:report result))]
    (doseq [part check-parts]
      (when-not (some #(.contains % part) errors)
        (throw (ex-info (format "Missing error '%s' in [%s]"
                                part
                                (->> (interpose ", " errors)
                                     (apply str)))
                        {}))))
    true))

(deftest resolve-test
  (is (= [1 2 3 4 5] (success '(map inc (range 5)))))
  (is (= "hello" (success '(str hello))))
  (is (= 0 (success '(+))))
  (is (= * (success '*)))
  (is (= ['simbal] (success '[simbal]))))

(deftest failed-resolve-test
  (is (failure '(/ 1 0) "Exception when resolving function call")))

(deftest resolve-macro-test
  (is (= [1 2 3 4 5] (success '(->> (range 5) (map inc)))))
  (is (= '->> (success '->>)))
  (is (= 6 (success '(->> (->> (range 3) (map inc))
                          (reduce +))))))
