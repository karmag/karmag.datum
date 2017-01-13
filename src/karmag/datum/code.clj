(ns karmag.datum.code
  (:refer-clojure :exclude [resolve])
  (:require [karmag.datum.traversal :refer [walk]]
            [karmag.datum.util :refer [mk-rep]]
            [karmag.datum.whitelist :refer :all]))

(def default-whitelist {:functions pure-function-whitelist
                        :macros pure-macro-whitelist})

(defn- resolve-run [state form]
  (try [state (apply (first form) (rest form))]
       (catch Throwable t
         [(mk-rep state :error "Exception when resolving function call"
                  :exception t
                  :form form)
          :karmag.datum.error/unresolved-code])))

(defn resolve
  ([form]
   (resolve form default-whitelist))
  ([form whitelist]
   (let [[state data]
         (walk {:report []}
               (fn [state item]
                 (cond
                   ;; macro
                   (and (list? item) (symbol? (first item)))
                   [state (macroexpand (get (:macros whitelist) item item))]
                   ;; function
                   (symbol? item)
                   [state (get (:functions whitelist) item item)]
                   ;; other
                   :else [state item]))
               (fn [state item]
                 (cond
                   (seq? item) (resolve-run state item)
                   :else [state item]))
               form)]
     (assoc state :result data))))
