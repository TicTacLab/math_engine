(ns malt.configurator
  (:require [compojure.core :refer (routes GET POST PUT PATCH DELETE wrap-routes)]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as res]
            [cheshire.core :as json]
            [ring.util.request :as req]
            [clojure.tools.trace :refer [trace]]
            [environ.core :as environ]
            [clojure.stacktrace :as exp]
            [criterium.core :refer [bench]]))

(defonce srv (atom nil))
(defonce config (atom (-> environ/env
                          (select-keys [:storage-nodes
                                        :storage-keyspace
                                        :storage-user
                                        :storage-password
                                        :configuration-table
                                        :zabbix-host
                                        :zabbix-port
                                        :monitoring-hostname
                                        :session-ttl
                                        :cache-on
                                        :rest-port])
                          (update-in [:storage-nodes] #(if (sequential? %)
                                                        %
                                                        (json/parse-string % true)))
                          (update-in [:cache-on] #(Boolean/valueOf %))
                          (update-in [:rest-port] #(Integer/valueOf %))
                          (update-in [:session-ttl] #(Integer/valueOf %))
                          (update-in [:zabbix-port] #(Integer/valueOf %)))))

(defn parse-config [req]
  (-> req
      (req/body-string)
      (json/parse-string true)))

(defn ok-response [config]
  (-> config
      (json/generate-string)
      (res/response)
      (res/content-type "application/json")))

(defn read-config []
  (ok-response @config))

(defn update-config [req]
  (as-> req $
        (parse-config $)
        (swap! config merge $)
        (ok-response $)))

(defn restart-system [restart-fn]
  (try
    (restart-fn)
    (res/status {} 200)
    (catch Exception e
      (-> (res/response (with-out-str
                          (exp/print-stack-trace e)))
          (res/status 500)))))

(defn handler [restart-fn]
  (routes
    (GET   "/config" req (read-config))
    (PATCH "/config" req (update-config req))

    (POST  "/config/apply" req (restart-system restart-fn))))

(defn start [restart-fn]
  (when-not @srv
    (reset! srv (jetty/run-jetty (handler restart-fn)
                                 {:host  "0.0.0.0"
                                  :port  1300
                                  :join? false}))))

(defn stop []
  (swap! srv #(when %
               (.stop %)
               nil)))

(defn restart [restart-fn]
  (stop)
  (start restart-fn))



