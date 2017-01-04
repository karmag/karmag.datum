(ns karmag.datum.code
  (:refer-clojure :exclude [resolve])
  (:require [karmag.datum.traversal :refer [walk]]
            [karmag.datum.whitelist :refer :all]))

(def default-whitelist pure-whitelist)

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
                   (symbol? item) [state (get whitelist item item)]
                   :else [state item]))
               (fn [state item]
                 (cond
                   (seq? item) (resolve-run state item)
                   :else [state item]))
               form)]
     (assoc state :result data))))
