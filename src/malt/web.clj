(ns malt.web
  (:require [compojure.core :refer (defroutes GET POST PUT DELETE wrap-routes)]
            [schema.core :as s]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [malt.storage.models :refer (get-model)]
            [malt.storage.calculation-log :as calc-log]
            [malt.calculator :refer [calc]]
            [malt.utils :refer (string-to-double string-to-integer)]
            [ring.middleware
             [params :refer (wrap-params)]
             [keyword-params :refer (wrap-keyword-params)]]
            [ring.util.request :as req]
            [ring.util.response :as res]
            [cheshire.core :as json]
            [malt.session :as session]
            [malcolmx.core :as malx]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.java.io :as io])
  (:import (org.eclipse.jetty.server Server)))

(defn param-to-value [{:keys [id value]}]
  {:value (try (string-to-double value)
               (catch Exception e value))
   :id    (string-to-integer id)})

(defn calc-handler [{{session-store :session-store} :web
                    {ssid :ssid} :params :as req}
                    & {:keys [profile?] :or {profile? false}}]
  (let [args (-> req
                 req/body-string
                 (json/parse-string true)
                 (update-in [:id] string-to-integer)
                 (update-in [:params] #(mapv param-to-value %))
                 (assoc :ssid ssid))
        result (or (calc session-store args :profile? profile?)
                   {:error "Service is busy"})]
    (calc-log/write! (:storage session-store) (:ssid args) (:id args) (:params args) result)
    (res/content-type {:body (json/generate-string result)}
                      "application/json")))

(defn models-in-params-handler [{web :web
                                 params :params :as req}]
  (let [{:keys [id ssid]} params
        model-id (Integer/valueOf ^String id)
        session (session/create-or-prolong (:session-store web) model-id ssid)
        workbook (:wb session)
        in-sheet-name (:in_sheet_name session)
        new-in-params (as-> workbook $
                            (malx/get-sheet $ in-sheet-name)
                            (map keywordize-keys $)
                            (map #(update-in % [:id] long) $))]
    (json/generate-string new-in-params)))

(defn destroy-session [{{sstore :session-store} :web
                        {ssid   :ssid}          :params}]
  (when-let [workbook-config (session/fetch sstore ssid)]
    (if (session/with-locked-workbook workbook-config
          (session/delete! sstore ssid))
      "ok"
      {:code 409
       :body (format "Workbook: %s calculation inprogress" (:id workbook-config))})))

(defroutes routes
  (GET "/model/in-params" req (models-in-params-handler req))
  (POST "/model/calc/:ssid" req (calc-handler req :profile? true))
  (POST "/model/calc/:ssid/binary" req (calc-handler req :profile? false))
  (DELETE "/session/:ssid" req (destroy-session req))
  (route/not-found "<h1>Page not found!</h1>"))

(defn wrap-with-web [h web]
  (fn [req]
    (h (assoc req :web web))))

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
      (.stop ^Server server)
      (log/info "Web service stopped"))
    (assoc component :server nil)))

(def WebSchema
  {:port s/Int
   :host s/Str})

(defn new-web [m]
  (as-> m $
        (select-keys $ (keys WebSchema))
        (s/validate WebSchema $)
        (map->Web $)))
