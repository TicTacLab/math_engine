(ns malt.main
  (:require [malt.system :as s]
            [com.stuartsierra.component :as component]
            [malt.configurator :as conf]
            [clojure.tools.logging :as log])
  (:gen-class))

(defonce system (atom nil))

(defn -main [& _args]
  (try
    (swap! system #(if % % (component/start (s/new-system @conf/config))))
    (catch Exception e
      (log/error e "Exception during startup. Fix configuration and
                    start application using REST configuration interface")))
  (conf/start (fn []
                (swap! system
                       (fn [s]
                         (component/stop s)
                         (component/start (s/new-system @conf/config))))))
  (.. Runtime
      (getRuntime)
      (addShutdownHook (Thread. (fn []
                                  (do
                                    (component/stop @system)
                                    (conf/stop)))))))
