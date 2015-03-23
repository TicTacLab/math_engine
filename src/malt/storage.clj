(ns malt.storage
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [clojurewerkz.cassaforte.policies :as cp]
            [clojure.tools.logging :as log]))

(defrecord Storage [conn
                    storage-nodes
                    storage-keyspace
                    storage-user
                    storage-password
                    configuration-table
                    cache-on]
  component/Lifecycle

  (start [component]
    (let [conn (cc/connect storage-nodes
                           storage-keyspace
                           {:credentials {:username storage-user
                                          :password storage-password}})]
      (log/info "Storage started")
      (assoc component :conn conn)))
  (cp/constant-reconnection-policy 100)
  (stop [component]
    (when conn
      (cc/disconnect conn))
    (log/info "Storage stopped")
    (assoc component :conn nil)))


(def StorageSchema
  {:storage-nodes             [s/Str]
   :storage-keyspace          s/Str
   :storage-user              s/Str
   :storage-password          s/Str
   :configuration-table       s/Str
   (s/optional-key :cache-on) (s/maybe s/Bool)})

(defn new-storage [m]
  (s/validate StorageSchema m)
  (map->Storage m))
