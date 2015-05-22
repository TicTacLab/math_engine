(ns malt.main
  (:require [malt.system :as s]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [noilly.core :as noilly])
  (:gen-class))

(defonce system (atom nil))
(defonce noilly-srv (atom nil))

(defn -main [& _args]
  (try
    (swap! system #(if % % (component/start (s/new-system @s/config))))
    (catch Exception e
      (log/error e "Exception during startup. Fix configuration and
                    start application using REST configuration interface")))
  (swap! noilly-srv
         (fn [srv]
           (if srv
             srv
             (noilly/start s/config
                           #(swap! system
                                   (fn [s]
                                     (component/stop s)
                                     (component/start (s/new-system @s/config))))))))
  (.. Runtime
      (getRuntime)
      (addShutdownHook (Thread. (fn []
                                  (do
                                    (component/stop @system)
                                    (noilly/stop @noilly-srv)))))))
