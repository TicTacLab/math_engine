(ns malt.api-test
  (:require [malt.system :as s]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [malt.config :as c]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [malt.web :as web])
  (:import (java.util UUID)))

(def invalid-model-id Integer/MAX_VALUE)
(def bolvanka-model-id 1000)
(def bolvanka-calculate-request-body (read-string (slurp "test/malt/bolvanka-calculate-request-body.edn")))
(def bolvanka-calculate-response-body (read-string (slurp "test/malt/bolvanka-calculate-response-body.edn")))
(def bolvanka-in-params-response-body (read-string (slurp "test/malt/bolvanka-in-params-response-body.edn")))
(def request-sent-delay-ms 1500)

(def system nil)

(defn start-system []
  (alter-var-root #'system (constantly (s/new-system (c/config))))
  (alter-var-root #'system component/start))

(defn stop-system []
  (when system
    (alter-var-root #'system component/stop)))

(defn wrap-with-system [f]
  (try
    (stop-system)
    (start-system)
    (f)
    (finally
      (stop-system))))

(defn make-url [model-id ssid action]
  (format "http://localhost:%s/models/%s/%s%s"
          (:port (c/config))
          model-id
          ssid
          action))

(defn make-calc-request [model-id event-id body]
  (http/post (make-url model-id event-id "/calculate")
             {:body    (json/generate-string body)
              :timeout 60000
              :as      :text}))

(defn response->status+result [{:keys [body status]}]
  [status (json/parse-string body true)])

(defn error-messages->blank [error-resp]
  (update-in error-resp [:errors] #(map (fn [error] (assoc error :message "")) %)))

(use-fixtures :each wrap-with-system)

(deftest resource-not-found
  (is (= [404 web/error-404-rnf]
         (->> "/invalid-url"
              (make-url invalid-model-id (UUID/randomUUID))
              (http/get)
              (deref)
              (response->status+result)))
      "should return json 404 error"))

(deftest in-params
  (is (= [404 web/error-404-fnf]
         (-> invalid-model-id
             (make-url (UUID/randomUUID) "/in-params")
             (http/get)
             (deref)
             (response->status+result)))
      "should fail to process invalid model")

  (is (= [200 bolvanka-in-params-response-body]
         (-> bolvanka-model-id
             (make-url (UUID/randomUUID) "/in-params")
             (http/get)
             (deref)
             (response->status+result)))
      "should return in-params"))

(deftest calculate-method
  (let [event-id (UUID/randomUUID)
        calc-request (make-calc-request bolvanka-model-id
                                        event-id
                                        (assoc bolvanka-calculate-request-body
                                               :event_id event-id
                                               :model_id bolvanka-model-id))]
    (Thread/sleep request-sent-delay-ms)                                     ;; ensure request sent

    (is (= [423 web/error-423-cip]
           (-> bolvanka-model-id
               (make-calc-request event-id (assoc bolvanka-calculate-request-body
                                                  :event_id event-id
                                                  :model_id bolvanka-model-id))

               (deref)
               (response->status+result)))
        "session should be locked")

    (is (= [200 bolvanka-calculate-response-body]
           (response->status+result @calc-request))
        "should calculate successfully")

    (are [status code p-model-id p-event-id b-model-id b-event-id body]
      (= [status (web/error-response status code "")]
         (-> (make-calc-request p-model-id p-event-id (assoc body
                                                             :model_id b-model-id
                                                             :event_id b-event-id))
             (deref)
             (response->status+result)
             (update-in [1] error-messages->blank)))

      400 "MFP" bolvanka-model-id "7" bolvanka-model-id "7" {}
      404 "MNF" invalid-model-id "6" invalid-model-id "6" bolvanka-calculate-request-body)))

(deftest profile-method
  (let [event-id (UUID/randomUUID)]
    (is (-> bolvanka-model-id
            (make-url event-id "/profile")
            (http/post {:body    (json/generate-string (assoc bolvanka-calculate-request-body
                                                              :model_id bolvanka-model-id
                                                              :event_id event-id))
                        :timeout 60000
                        :as      :text})
            (deref)
            (response->status+result)
            (second)
            :data
            (#(every? :timer %)))
        "every outcome should have timer")))

(deftest release-method
  (is (= 204
         (-> bolvanka-model-id
             (make-url (UUID/randomUUID) "")
             (http/delete)
             (deref)
             :status))
      "should delete non existing valid session")

  (is (= 204
         (-> invalid-model-id
             (make-url (UUID/randomUUID) "")
             (http/delete)
             (deref)
             :status))
      "should delete non existing invalid session")

  (testing "Calculation in progress error"
    (let [event-id (UUID/randomUUID)
          calc-request (make-calc-request bolvanka-model-id event-id (assoc bolvanka-calculate-request-body
                                                                            :event_id event-id
                                                                            :model_id bolvanka-model-id))]
      (Thread/sleep request-sent-delay-ms)                                  ;; ensure request sent

      (is (= web/error-423-cip
             (-> bolvanka-model-id
                 (make-url event-id "")
                 (http/delete)
                 (deref)
                 :body
                 (json/parse-string true))))

      (is (= 200 (:status @calc-request))))))