(ns karmag.datum.core
  (:refer-clojure :exclude [read read-string])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set]
            [karmag.datum.code :as code]
            [karmag.datum.traversal :refer [walk walk-post]])
  (:import java.io.PushbackReader))

(declare expand-single)

(defrecord Def [id value default])
(defrecord Ref [id args])
(defrecord Arg [key])
(defrecord Code [form])

;; TODO do not allow more keys than what is recognized by parser

(def default-read-config
  {:readers {'def #(if (map? %)
                     (Def. (:id %) (:value %) (:default %))
                     (Def. (first %) (second %) (nth % 2 nil)))
             'ref #(cond
                     (vector? %) (Ref. (first %) (second %))
                     (map? %) (Ref. (:id %) (:args %))
                     :else (Ref. % nil))
             'arg ->Arg
             'code ->Code}
   :default vector})

;;--------------------------------------------------
;; utils

(defn- def? [x] (instance? Def x))
(defn- ref? [x] (instance? Ref x))
(defn- arg? [x] (instance? Arg x))
(defn- code? [x] (instance? Code x))

(defn- mk-rep [state severity msg & {:as data}]
  {:pre [(#{:info :warn :error} severity)]}
  (update-in state [:report] conj {:message msg
                                   :severity severity
                                   :data data}))

;;--------------------------------------------------
;; read

(defn read
  "Takes a reader and optional user config. The reader accepts any
  type that clojure.java.io/reader accepts. The user config may be
  used to specify readers.

  Returns a collection of items."
  [r & [user-config]]
  (with-open [reader (io/reader r)
              pbr (PushbackReader. reader)]
    (let [opts (merge default-read-config user-config {:eof ::eof})]
      (->> #(edn/read opts pbr)
           repeatedly
           (take-while #(not= ::eof %))
           doall))))

(defn from-string
  "Creates a reader from the given string data that can be passed to
  'read'."
  [s]
  (java.io.StringReader. s))

;;--------------------------------------------------
;; traversal

(defn- add-arg-usage [state key]
  (update-in state [:arg-stack]
             (fn [[head & tail]]
               (cons (update-in head [:used-args] conj key)
                     tail))))

(defn- validate-arg-usage [state]
  (let [arg-stack (-> state :arg-stack first)
        actual (-> arg-stack :mapping keys set)
        expected (-> arg-stack :used-args set)
        missing (sort-by str (clojure.set/difference expected actual))
        extra (sort-by str (clojure.set/difference actual expected))
        state (if (empty? missing)
                state
                (mk-rep state :warn (str "No replacement defined for " missing)
                        :item (:item arg-stack)
                        :actual actual
                        :expected expected))]
    (if (empty? expected)
      state
      (mk-rep state :warn (str "Extraneous arguments " extra)
              :item (:item arg-stack)
              :actual actual
              :expected expected))))

(defn- pre-expand [state item]
  (let [state (update-in state [:depth] inc)]
    (cond
      (ref? item) (let [definition (get-in state [:defs (:id item)])
                        args (merge (:default definition) (:args item))
                        state (update-in state [:arg-stack]
                                         conj {:item item
                                               :depth (:depth state)
                                               :mapping args})]
                    (expand-single state (:value definition)))
      (arg? item) (let [mapping (-> state :arg-stack first :mapping)
                        state (add-arg-usage state (:key item))]
                    (expand-single state (get mapping (:key item))))
      :else [state item])))

(defn- post-expand [state item]
  (let [state (update-in state [:depth] dec)
        state (if (= (:depth state) (-> state :arg-stack first :depth))
                (-> (validate-arg-usage state)
                    (update-in [:arg-stack] next))
                state)]
    (cond
      (code? item) (let [result (code/resolve (:form item))]
                     (if (empty? (:report result))
                       [state (:result result)]
                       [(mk-rep state :error "Code expansion failed"
                                :errors (:report result))
                        (:result result)]))
      ;; TODO replace :report with :logs
      :else [state item])))

(defn- expand-single
  "Applies expansion to the given item. Returns [state, item]."
  [state item]
  (walk state pre-expand post-expand item))

;;--------------------------------------------------
;; build

(defn- separate-defs
  "Extracts definitions as a sequence. Definitions found are removed
  from the item listing. Non-root definitions are replaced with nil."
  [state]
  (let [add-def #(update-in %1 [:defs] conj %2)]
    (-> (reduce
         (fn [state original-item]
           (first
            (walk-post state
                       (fn [state item]
                         (if (def? item)
                           (if (= original-item item)
                             [(add-def state item) item]
                             [(-> (add-def state item)
                                  (mk-rep :warn "Definition in non-root position"
                                          :root-item original-item
                                          :item item))
                              nil])
                           [state item]))
                       original-item)))
         state
         (:items state))
        (update-in [:items] (partial filter (comp not def?))))))

(defn- aggregate-defs
  "Make definitions lookup friendly."
  [state]
  (reduce (fn [state def]
            (if-let [old (get-in state [:defs (:id def)])]
              (-> (assoc-in state [:defs (:id def)] def)
                  (mk-rep :error "Duplicate definition"
                          :items [old def]))
              (assoc-in state [:defs (:id def)] def)))
          (assoc state :defs {})
          (:defs state)))

(defn- expand-items [state]
  (let [[state items] (expand-single state (:items state))]
    (assoc state :items items)))

(defn build [xs]
  (-> {:items xs, :defs [], :report [], :depth 0}
      separate-defs
      aggregate-defs
      expand-items))

;;--------------------------------------------------
;; extract

;; TODO remove this
(defn extract [data pred]
  (filter pred (:items data)))
