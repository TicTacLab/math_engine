(ns malt.main
  (:require [environ.core :as environ]
            [malt.system :as s]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defonce system nil)

(defn -main [& _args]
  (alter-var-root #'system (constantly (component/start (s/new-system environ/env))))
  (.. Runtime
      (getRuntime)
      (addShutdownHook (Thread. #(component/stop system)))))
