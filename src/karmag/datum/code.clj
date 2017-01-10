(ns karmag.datum.code
  (:refer-clojure :exclude [resolve])
  (:require [karmag.datum.traversal :refer [walk]]
            [karmag.datum.whitelist :refer :all]))

(def default-whitelist {:functions pure-function-whitelist
                        :macros pure-macro-whitelist})

(defn- mk-rep [state severity msg & {:as data}]
  {:pre [(#{:info :warn :error} severity)]}
  (update-in state [:report] conj {:message msg
                                   :severity severity
                                   :data data}))

(defn- resolve-run [state form]
  (try [state (apply (first form) (rest form))]
       (catch Throwable t
         [(mk-rep state :error "Exception when resolving function call"
                  :exception (Throwable->map t)
                  :form form)
          form])))

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
