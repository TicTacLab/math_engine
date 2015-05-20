(ns malt.system
  (:require
    [com.stuartsierra.component :as component]
    [zabbix-clojure-agent.core :as zabbix]
    [clojure.string :refer (split)]
    [malt.session :as session]
    [malt.storage :as storage]
    [malt.web :as w]
    [cheshire.core :as json]))

(defn new-system [config]
  (let [{:keys [rest-port monitoring-hostname zabbix-host zabbix-port storage-nodes session-ttl]} config]
    (component/map->SystemMap
      {:storage         (storage/new-storage (-> config
                                                 (select-keys [:storage-nodes :storage-keyspace :configuration-table
                                                               :storage-user :storage-password :cache-on])
                                                 (assoc :storage-nodes (json/parse-string storage-nodes))
                                                 (update-in [:cache-on] #(Boolean/valueOf %))))
       :session-store   (component/using
                          (session/new-session-store {:session-ttl (Integer/valueOf session-ttl)})
                          [:storage])
       :web             (component/using
                          (w/new-web {:host "0.0.0.0" :port (Integer/valueOf rest-port)})
                          [:storage :session-store])
       :zabbix-reporter (zabbix/new-zabbix-reporter
                          {:hostname         monitoring-hostname
                           :zabbix-host      zabbix-host
                           :zabbix-port      (Integer/valueOf zabbix-port)
                           :interval-minutes 1})})))
