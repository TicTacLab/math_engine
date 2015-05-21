(ns malt.configurator
  (:require [compojure.core :refer (routes GET POST PUT PATCH DELETE wrap-routes)]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as res]
            [cheshire.core :as json]
            [ring.util.request :as req]
            [clojure.tools.trace :refer [trace]]
            [clojure.stacktrace :as exp]
            [dire.core :refer [with-handler!]])
  (:import [com.fasterxml.jackson.core JsonParseException]))

(defonce srv (atom nil))
(def config (atom (json/parse-string (slurp "config.json") true)))

(defn json-response
  "Takes map, returns ring response with body as map in json
   and response type set to application/json."
  [m]
  (-> m
      (json/generate-string)
      (res/response)
      (res/content-type "application/json")))

(defn read-config []
  (-> @config
      (json-response)
      (res/status 200)))

(defn update-config [req]
  (as-> req $
        (req/body-string $)
        (json/parse-string $ true)
        (swap! config merge $)
        (json-response $)
        (res/status $ 200)))

(with-handler! #'update-config
  JsonParseException
  (fn [e _]
    (-> {:_error (.getMessage e)}
        (json-response)
        (res/status 400))))

(defn restart-system [restart-fn]
  (restart-fn)
  (-> {:_restarted :ok}
      (json-response)
      (res/status 200)))

(with-handler! #'restart-system
  Exception
  (fn [e _]
    (-> {:_error (.getMessage (exp/root-cause e))}
        (json-response)
        (res/status 500))))

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



