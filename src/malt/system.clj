(ns malt.system
  (:require 
   [com.stuartsierra.component :as component]
   [clojure.string :refer (split)]
   [malt.session :as s]
   [malt.storage :as storage]
   [malt.web :as w]
   [malt.storage.configuration :as config]))

(defn cast-to-vector [hosts-str]
  (remove empty? (split hosts-str #",")))

(defn new-system 
  [{:keys [storage-nodes] :as config}]
  (let [storage (select-keys config [:storage-nodes :storage-keyspace :configuration-table
                                     :storage-user :storage-password])
        storage-nodes (cast-to-vector storage-nodes)
        storage-component (storage/new-storage (assoc storage :storage-nodes storage-nodes))
        configuration (->> storage-component component/start config/read-config)
        _ (component/stop storage-component)
        {:keys [session-ttl cache-on rest-port]} configuration]
    (component/map->SystemMap
      {:storage       (storage/new-storage (assoc storage
                                             :cache-on cache-on
                                             :storage-nodes storage-nodes))

       :session-store (component/using
                        (s/new-session-store {:session-ttl session-ttl})
                        [:storage])
       :web           (component/using
                        (w/new-web {:host "0.0.0.0" :port rest-port})
                        [:storage :session-store])})))
