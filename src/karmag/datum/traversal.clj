(ns karmag.datum.traversal)

(declare walk)

(def ^:private noop vector)

(defn- walk-record [user-state pre post data]
  (reduce (fn [[user-state r] [k v]]
            (let [[user-state v] (walk user-state pre post v)]
              [user-state (assoc r k v)]))
          [user-state data]
          data))

(defn- walk-map [user-state pre post data]
  (reduce (fn [[user-state m] [k v]]
            (let [[user-state k] (walk user-state pre post k)
                  [user-state v] (walk user-state pre post v)]
              [user-state (assoc m k v)]))
          [user-state {}]
          data))

(defn- walk-vector [user-state pre post data]
  (reduce (fn [[user-state v] item]
            (let [[user-state item] (walk user-state pre post item)]
              [user-state (conj v item)]))
          [user-state []]
          data))

(defn- walk-list [user-state pre post data]
  (reduce (fn [[user-state l] item]
            (let [[user-state item] (walk user-state pre post item)]
              [user-state (conj l item)]))
          [user-state ()]
          (reverse data)))

(defn- walk-set [user-state pre post data]
  (reduce (fn [[user-state s] item]
            (let [[user-state item] (walk user-state pre post item)]
              [user-state (conj s item)]))
          [user-state #{}]
          data))

(defn walk [user-state pre post data]
  (let [[user-state data] (pre user-state data)
        [user-state data] (cond
                            (record? data) (walk-record user-state pre post data)
                            (map? data)    (walk-map user-state pre post data)
                            (vector? data) (walk-vector user-state pre post data)
                            (seq? data)    (walk-list user-state pre post data)
                            (set? data)    (walk-set user-state pre post data)
                            :else          [user-state data])]
    (post user-state data)))

(defn walk-pre [user-state pre data]
  (walk user-state pre noop data))

(defn walk-post [user-state post data]
  (walk user-state noop post data))
