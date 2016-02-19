(ns malt.system
  (:require
    [com.stuartsierra.component :as component]
    [malt.session :as session]
    [malt.storage :as storage]
    [malt.web :as w]
    [malt.monitoring :as mon]
    [malt.cache :as cache]))

(defn new-system [config]
  (component/system-map
    :cache (cache/new-cache config)
    :storage (storage/new-storage config)
    :session-store (component/using
                     (session/new-session-store config)
                     [:storage :cache])
    :web (component/using
           (w/new-web config)
           [:storage :session-store])
    :jmx-reporter (mon/new-jmx-reporter)))

