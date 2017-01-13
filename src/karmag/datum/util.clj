(ns karmag.datum.util)

(defn- exception-usage-ok [data]
  (or (nil? (find data :exception))
      (instance? Throwable (:exception data))))

(defn mk-rep [state severity msg & {:as data}]
  {:pre [(#{:info :warn :error} severity)
         (exception-usage-ok data)]}
  (update-in state [:report] conj {:message msg
                                   :severity severity
                                   :data data}))
