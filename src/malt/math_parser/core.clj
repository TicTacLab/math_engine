(ns malt.math_parser.core
  (:require
    [clojure.tools.logging :as logger :only (info error)]
    [clojure.tools.trace :refer (trace)]
    [malt.math_parser.xls_read :refer (make-formula-evaluator extract-eval)]
    [malt.math_parser.params :refer [set-params]]
    [malt.utils :as utils]
    [malt.session :as session]
    [malt.storage :as storage]
    [malt.storage.cache :as cache]
    [malt.storage.models :as models]
    [malt.response :as response]
    [flatland.protobuf.core :as pb]
    [metrics.meters :as meter])
  (:import [malt.session WorkbookConfig]))

(defn #^WorkbookConfig make-evaluator [#^WorkbookConfig wb-config]
  (let [evaluator (->> wb-config :wb make-formula-evaluator)]
    (assoc wb-config :evaluator evaluator)))

(defn calc* [session-store
             {id :id ssid :ssid params :params}
             & {calc-profile :calc-profile :or {calc-profile false}}]
  (let [workbook-config (session/create-if-not-exists session-store id ssid)]
    (if-let [result (session/with-locked-workbook workbook-config
                      (let [wb (->> workbook-config
                                    (set-params params)
                                    make-evaluator)
                            result (extract-eval wb calc-profile)]
                        (session/save! session-store ssid wb)
                        result))]
      result
      {:type       :ERROR
       :error_type :INPROGRESS
       :error      (str "Workbook: " id " calculation inprogress")})))

(defn calc [{storage :storage :as session-store}
            {id :id ssid :ssid params :params :as args}
            & {calc-profile :calc-profile}]
  (meter/mark! (:calls storage))
  (when (session/fetch session-store ssid)
    (session/prolong! session-store ssid))
  (cache/with-cache-by-key storage {:id id :params params}
    (if calc-profile
      (response/packet-init (utils/with-timer (calc* session-store args :calc-profile calc-profile)))
      (response/packet-init (calc* session-store args :calc-profile calc-profile)))))
