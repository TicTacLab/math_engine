(ns malt.web
  (:require [compojure.core :refer (defroutes ANY GET POST PUT DELETE wrap-routes)]
            [schema.core :as s]
            [ring.adapter.jetty :as jetty]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
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
            [malt.storage.models :as models])
  (:import (org.eclipse.jetty.server Server)))

(defn json-error [status code message]
  {:status status
   :body (json/generate-string {:status status
                                :errors [{:code    code
                                          :message message}]})})

(defn json-result [status result]
  {:status status
   :body (json/generate-string {:status status
                                :data result})})

(def error-400-mfp (json-error 400 "MFP" "Malformed params"))
(def error-404-mnf (json-error 404 "MNF" "Model not found"))
(def error-404-rnf (json-error 404 "RNF" "Resource not found"))
(def error-423-cip (json-error 423 "CIP" "Calculation is in progress"))
(def error-500-ise (json-error 500 "ISE" "Internal server error"))

;; FIXME: purpose?
(defn coerce-params-fields [{:keys [id value]}]
  {:value (try (string-to-double value)
               (catch Exception _ value))
   :id    (string-to-integer id)})

(def calculate-body-schema {:model_id s/Int
                              :event_id s/Str
                              :params [{:id s/Str
                                        :value s/Str}]})
(defn calc-handler [{{session-store :session-store
                      storage :storage} :web
                     :as req}
                    & {:keys [profile?] :or {profile? false}}]
  (let [json-body (json/parse-string (req/body-string req) true)
        model-id (:model_id json-body)]
    (if (models/valid-model? storage model-id)
      (if-let [check-result (s/check calculate-body-schema json-body)]
        (do
          (log/error "Malformed params for CALCULATE request: %s, reason %s" json-body check-result)
          error-400-mfp)
        (let [args (update-in json-body [:params] #(mapv coerce-params-fields %))]
          (if-let [result (calc session-store args :profile? profile?)]
            (do
              (calc-log/write! (:storage session-store) (:ssid args) (:id args) (:params args) result)
              (json-result 200 result))
            error-423-cip)))
      error-404-mnf)))


(defn models-in-params-handler [{{storage :storage :as web}  :web
                                 params :params}]
  (let [{:keys [model-id ssid]} params
        model-id (Integer/valueOf ^String model-id)]
    (if (models/valid-model? storage model-id)
      (let [session (session/create-or-prolong (:session-store web) model-id ssid)
            workbook (:wb session)
            in-sheet-name (:in_sheet_name session)
            in-params (as-> workbook $
                            (malx/get-sheet $ in-sheet-name)
                            (map keywordize-keys $)
                            (map #(update-in % [:id] long) $))]
        (json/generate-string {:status 200
                               :data   in-params}))
      error-404-mnf)))

(defn destroy-session [{{sstore :session-store} :web
                        {ssid :ssid}            :params}]
  ;; omg, ugliest code ever
  (if-let [workbook-config (session/fetch sstore ssid)]
    (if (session/with-locked-workbook workbook-config
                                      (session/delete! sstore ssid))
      {:status 204}
      error-423-cip)
    {:status 204}))

(defroutes routes
  (GET "/models/:model-id/:ssid/in-params" req (models-in-params-handler req))
  (POST "/models/:model-id/:ssid/profile" req (calc-handler req :profile? true))
  (POST "/models/:model-id/:ssid/calculate" req (calc-handler req :profile? false))
  (DELETE "/models/:model-id/:ssid" req (destroy-session req))
  (ANY "/*" _ error-404-rnf))

(defn wrap-with-web [h web]
  (fn [req]
    (h (assoc req :web web))))

(defn wrap-internal-server-error [h]
  (fn [req]
    (try
      (h req)
      (catch Exception e
        (log/error e "while request handling")
        error-500-ise))))

(defn wrap-json-content-type [h]
  (fn [req]
    (-> req
        (h)
        (res/content-type "application/json")
        (res/charset "utf-8"))))

(defn app [web]
  (-> routes
      (wrap-keyword-params)
      (wrap-params)
      (wrap-with-web web)
      (wrap-internal-server-error)
      (wrap-json-content-type)))

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
