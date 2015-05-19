(ns malt.storage.cache
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [taoensso.nippy :as nippy]
            [metrics.meters :as meter]
            [clojure.tools.logging :as log])
  (:import (com.datastax.driver.core.utils Bytes)
           [com.datastax.driver.core.exceptions DriverException]))

(defn put [{conn :conn} {model-id :id rev :rev params :params} value]
  (println rev)
  (try
    (cql/insert conn "caches" {:model_id model-id
                              :params   (nippy/freeze params)
                              :result   (nippy/freeze value)
                              :rev      rev})
    (catch DriverException e
      (log/error e "Exception during cache fetching")
      nil)))

(defn fetch [{conn :conn} {model-id :id rev :rev params :params}]
  (try
    (let [bs (:result (cql/get-one conn "caches"
                                   (columns :result)
                                   (where [[= :model_id model-id]
                                           [= :params (nippy/freeze params)]
                                           [= :rev rev]])))]
      (when bs
        (nippy/thaw (Bytes/getArray bs))))
    (catch DriverException e
      (log/error e "Exception during cache fetching")
      nil)))

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