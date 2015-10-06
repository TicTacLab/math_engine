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

(defn error-response [status code message]
  {:status status
   :errors [{:code    code
             :message message}]})

(defn success-response [status result]
  {:status status
   :data   result})

(defn response->json-response [json]
  {:status (:status json)
   :body (json/generate-string json)})

(def error-404-mnf (error-response 404 "MNF" "Model not found"))
(def error-404-rnf (error-response 404 "RNF" "Resource not found"))
(def error-423-cip (error-response 423 "CIP" "Calculation is in progress"))
(def error-500-ise (error-response 500 "ISE" "Internal server error"))

;; FIXME: purpose?
(defn coerce-params-fields [{:keys [id value]}]
  {:value (try (string-to-double value)
               (catch Exception _ value))
   :id    (string-to-integer id)})

(defn try-string->int [value]
  (try
    (Integer/valueOf ^String value)
    (catch Exception _
      value)))

(defn try-string->json [value]
  (try
    (json/parse-string value true)
    (catch Exception _
      value)))

(defn return-with-log [value msgf & args]
  (log/info (apply format (cons msgf args)))
  value)

(defn calculate-model-out-values [session-store model-id event-id params profile?]
  (let [params (mapv coerce-params-fields params)] ;; FIXME: remove coersion?
    (if-let [result (calc session-store model-id event-id params profile?)]
      (do
        (calc-log/write! (:storage session-store) model-id event-id params result)
        (success-response 200 result))
      (return-with-log error-423-cip
                      "Calculation in progress for request: %s %s"
                      model-id
                      event-id
                      params))))

(def calc-handler-body-schema {:model_id s/Int
                               :event_id s/Str
                               :params [{:id s/Str
                                         :value s/Str}]})
(def calc-handler-params-schema {:model-id s/Int
                                 :event-id s/Str
                                 s/Keyword s/Any})
(defn calc-handler [{{session-store :session-store
                      storage :storage} :web
                     request-params :params
                     :as req}
                    & {:keys [profile?] :or {profile? false}}]
  (let [json-body (try-string->json (req/body-string req))
        {:keys [model-id event-id] :as params} (update-in request-params [:model-id] try-string->int)
        params-checking-result (s/check calc-handler-params-schema params)
        json-body (assoc json-body :model_id model-id :event_id event-id)
        json-checking-result (s/check calc-handler-body-schema json-body)]

    (response->json-response
      (cond
        params-checking-result (return-with-log (error-response 400 "MFP" "Malformed params")
                                               "Malformed params for CALCULATE request: %s, reason %s"
                                               request-params
                                               params-checking-result)
        json-checking-result (return-with-log (error-response 400 "MFP" "Malformed body")
                                             "Malformed body for CALCULATE request: %s, reason %s"
                                             json-body
                                             json-checking-result)
        (not= model-id (:model_id json-body)) (return-with-log (error-response 400 "MFP" "Model ids mismatch in body and params")
                                                              "Model ids mismatch in body and params")
        (not= event-id (:event_id json-body)) (return-with-log (error-response 400 "MFP" "Event ids mismatch in body and params")
                                                              "Event ids mismatch in body and params")
        (not (models/valid-model? storage model-id)) (return-with-log error-404-mnf
                                                                     "Invalid model id %d"
                                                                     model-id)
        :else (calculate-model-out-values session-store model-id event-id (:params json-body) profile?)))))


(defn get-model-in-params [web model-id event-id plain?]
  (let [session (session/create-or-prolong (:session-store web) model-id event-id)
        workbook (:wb session)
        header (malx/get-sheet-header workbook (:out_sheet_name session))
        data (as-> workbook $
                   (malx/get-sheet $ (:in_sheet_name session))
                   (map #(update-in % ["id"] long) $))]
    (if-not plain?
      data
      (concat [header] (mapv (fn [row] (mapv #(get row %) header)) data)))))

(def in-params-handler-params-schema {:model-id s/Int
                                      :event-id s/Str
                                      s/Keyword s/Any})
(defn in-params-handler [{{storage :storage :as web}  :web
                          request-params :params}]
  (let [params (update-in request-params [:model-id] try-string->int)
        {:keys [model-id event-id plain]} params
        plain? (Boolean/valueOf plain)
        params-checking-result (s/check in-params-handler-params-schema params)]
    (response->json-response
      (cond
        params-checking-result (return-with-log (error-response 400 "MFP" "Malformed params")
                                                "Malformed params for IN-PARAMS request: %s, reason %s"
                                                request-params
                                                params-checking-result)
        (not (models/valid-model? storage model-id)) (return-with-log error-404-mnf
                                                                      "Invalid model id %d"
                                                                      model-id)
        :else (success-response 200 (get-model-in-params web model-id event-id plain?))))))

(defn get-model-out-values-header [web model-id event-id]
  (let [session (session/create-or-prolong (:session-store web) model-id event-id)]
    (as-> (:wb session) $
          (malx/get-sheet-header $ (:out_sheet_name session)))))

(def out-values-header-handler-params-schema {:model-id s/Int
                                      :event-id s/Str
                                      s/Keyword s/Any})
(defn out-values-header-handler [{{storage :storage :as web}  :web
                          request-params :params}]
  (let [params (update-in request-params [:model-id] try-string->int)
        {:keys [model-id event-id]} params
        params-checking-result (s/check out-values-header-handler-params-schema params)]
    (response->json-response
      (cond
        params-checking-result (return-with-log (error-response 400 "MFP" "Malformed params")
                                                "Malformed params for IN-PARAMS request: %s, reason %s"
                                                request-params
                                                params-checking-result)
        (not (models/valid-model? storage model-id)) (return-with-log error-404-mnf
                                                                      "Invalid model id %d"
                                                                      model-id)
        :else (success-response 200 (get-model-out-values-header web model-id event-id))))))

(defn destroy-session [{{sstore :session-store} :web
                        {event-id :event-id}    :params}]
  (let [workbook-config (session/fetch sstore event-id)]
    (if (or (not workbook-config)
            (session/with-locked-workbook workbook-config
                                          (session/delete! sstore event-id)))
      {:status 204}
      (response->json-response error-423-cip))))

(defn wrap-with-web [h web]
  (fn [req]
    (h (assoc req :web web))))

(defn wrap-internal-server-error [h]
  (fn [req]
    (try
      (h req)
      (catch Exception e
        (log/error e "while request handling")
        (response->json-response error-500-ise)))))

(defn wrap-json-content-type [h]
  (fn [req]
    (-> req
        (h)
        (res/content-type "application/json")
        (res/charset "utf-8"))))

(defroutes routes
  (GET "/files/:model-id/:event-id/in-params" req (in-params-handler req))
  (GET "/files/:model-id/:event-id/out-values-header" req (out-values-header-handler req))
  (POST "/files/:model-id/:event-id/profile" req (calc-handler req :profile? true))
  (POST "/files/:model-id/:event-id/calculate" req (calc-handler req :profile? false))
  (DELETE "/files/:model-id/:event-id" req (destroy-session req))
  (ANY "/*" _ (response->json-response error-404-rnf)))

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
