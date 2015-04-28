(ns malt.web
  (:require [compojure.core :refer (defroutes GET POST PUT DELETE wrap-routes)]
            [schema.core :as s]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojure.tools.trace :refer (trace)]
            [malt.web.middleware :refer (wrap-with-web)]
            [malt.storage.models :refer (get-model-file)]
            [malt.storage.calculation-log :as calc-log]
            [malt.math-parser.core :refer (calc)]
            [malt.utils :refer (string-to-double string-to-integer)]
            [ring.middleware
             [params :refer (wrap-params)]
             [keyword-params :refer (wrap-keyword-params)]]
            [ring.util.request :as req]
            [cheshire.core :as json]
            [clojure.pprint :refer (pprint)]
            [malt.session :as session]
            [clojure.java.io :as io]))

(defn param-to-value [{:keys [id value]}]
  {:value (try (string-to-double value)
               (catch Exception e value))
   :id    (string-to-integer id)})

(defn calc-handler [{{session-store :session-store} :web :as req}
                    & {:keys [calc-profile] :or {calc-profile false}}]
  (let [args (-> req
                 req/body-string
                 (json/parse-string true)
                 (update-in [:id] #(Integer. %))
                 (update-in [:params] #(mapv param-to-value %)))
        result (or (calc session-store args :calc-profile calc-profile)
                   {:error "Service is busy"})]
    (calc-log/write! (:storage session-store) (:ssid args) (:id args) (:params args) result)
    {:body (io/input-stream result)}))

(defn models-in-params-handler [{web :web
                                 params :params :as req}]
  (let [{:keys [id ssid]} params 
        model-id (Integer. id)]
    (some->>
     (session/create-if-not-exists (:session-store web) model-id ssid)
     :params
     (map (fn [param] (-> param val first (select-keys [:type :name :code :id :value]))))
     json/generate-string)))


(defroutes routes

  (GET "/model/in-params" req (models-in-params-handler req))

  (POST   "/model/calc" req (calc-handler req :calc-profile true))
  (POST   "/model/calc/binary" req (calc-handler req :calc-profile false))
  (route/not-found "<h1>Page not found!</h1>"))

(defn app [web]
  (-> routes
      (wrap-keyword-params)
      (wrap-params)
      (wrap-with-web web)))

(defrecord Web [host port server storage api]
  component/Lifecycle

  (start [component]
    (let [srv (jetty/run-jetty (app component) {:port port
                                                :host host
                                                :join? false})]
      (log/info "Web service started at:" (str host ":" port))
      (assoc component :server srv)))

  (stop [component]
    (when server
      (.stop server)
      (log/info "Web service stopped"))
    (assoc component :server nil)))

(def WebSchema
  {:port s/Int
   :host s/Str})

(defn new-web [m]
  (s/validate WebSchema m)
  (map->Web m))
