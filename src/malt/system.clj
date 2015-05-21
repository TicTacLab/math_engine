(ns malt.system
  (:require
    [com.stuartsierra.component :as component]
    [zabbix-clojure-agent.core :as zabbix]
    [clojure.string :refer (split)]
    [malt.session :as session]
    [malt.storage :as storage]
    [malt.web :as w]))

(defn new-system [config]
  (let [{:keys [monitoring-hostname zabbix-host zabbix-port]} config]
    (component/map->SystemMap
      {:storage         (storage/new-storage config)
       :session-store   (component/using
                          (session/new-session-store config)
                          [:storage])
       :web             (component/using
                          (w/new-web config)
                          [:storage :session-store])
       :zabbix-reporter (zabbix/new-zabbix-reporter
                          {:hostname         monitoring-hostname
                           :zabbix-host      zabbix-host
                           :zabbix-port      (Integer/valueOf zabbix-port)
                           :interval-minutes 1})})))
