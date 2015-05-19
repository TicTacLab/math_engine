(ns malt.math-parser.core
  (:require
    [clojure.tools.logging :as logger :only (info error)]
    [clojure.tools.trace :refer (trace)]
    [malt.math-parser.xls-read :refer (make-formula-evaluator extract-eval)]
    [malt.math-parser.params :refer [set-params]]
    [malt.utils :as utils]
    [malt.session :as session]
    [malt.storage :as storage]
    [malt.storage
     [cache :as cache]
     [models :as models]
     [in-params :as in-params]]
    [malt.response :as response]
    [flatland.protobuf.core :as pb]
    [metrics.meters :as meter])
  (:import [malt.session WorkbookConfig]))

(defn #^WorkbookConfig make-evaluator [#^WorkbookConfig wb-config]
  (let [evaluator (->> wb-config :wb make-formula-evaluator)]
    (assoc wb-config :evaluator evaluator)))

(defn calc* [workbook-config
             {id :id params :params}
             & {calc-profile :calc-profile :or {calc-profile false}}]
  (if-let [result (session/with-locked-workbook workbook-config
                    (as-> workbook-config $
                          (set-params params $)
                          (make-evaluator $)
                          (extract-eval $ calc-profile)))]
    result
    {:type       :ERROR
     :error_type :INPROGRESS
     :error      (str "Workbook: " id " calculation inprogress")}))

(defn calc [{storage :storage :as session-store}
            {id :id ssid :ssid params :params :as args}
            & {calc-profile :calc-profile}]
  (meter/mark! (:calls storage))
  (in-params/write! storage id params)
  (let [workbook-config (session/create-or-prolong session-store id ssid)
        rev (:rev workbook-config)]
    (cache/with-cache-by-key storage {:id id :rev rev :params params}
      (if calc-profile
        (response/packet-init (utils/with-timer (calc* workbook-config args :calc-profile calc-profile)))
        (response/packet-init (calc* workbook-config args :calc-profile calc-profile))))))
