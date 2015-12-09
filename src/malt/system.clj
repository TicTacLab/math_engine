(ns malt.system
  (:require
    [com.stuartsierra.component :as component]
    [malt.session :as session]
    [malt.storage :as storage]
    [malt.web :as w]
    [malt.monitoring :as mon]))

(defn new-system [config]
  (component/system-map
    :storage (storage/new-storage config)
    :session-store (component/using
                     (session/new-session-store config)
                     [:storage])
    :web (component/using
           (w/new-web config)
           [:storage :session-store])
    :jmx-reporter (mon/new-jmx-reporter)))