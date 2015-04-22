(ns malt.storage.cache
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [taoensso.nippy :as nippy]
            [metrics.meters :as meter])
  (:import (com.datastax.driver.core.utils Bytes)))

(defn put [{conn :conn} {model-id :id params :params} value]
  (cql/insert conn "cache" {:model_id model-id
                            :params   (nippy/freeze params)
                            :result   (nippy/freeze value)}))

(defn fetch [{conn :conn} {model-id :id params :params}]
  (let [bs (-> (cql/select conn "cache"
                               (columns :result)
                               (where [[= :model_id model-id]
                                       [= :params (nippy/freeze params)]]))
                   (first)
                   (:result))]
    (when bs
      (nippy/thaw (Bytes/getArray bs)))))

(defmacro with-cache-by-key [storage key & body]
  `(if (:cache-on ~storage)
     (if-let [result# (fetch ~storage ~key)]
       (do
         (meter/mark! (:hits ~storage))
         result#)
      (when-let [result# (do ~@body)]
        (put ~storage ~key result#)
        result#))
     (do ~@body)))