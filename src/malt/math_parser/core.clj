(ns malt.math-parser.core
  (:require
    [clojure.tools.logging :as logger :only (info error)]
    [malt.math-parser.params :refer [set-params]]
    [malt.utils :as utils]
    [malt.session :as session]
    [malt.storage :as storage]
    [malt.storage
     [cache :as cache]
     [models :as models]
     [in-params :as in-params]]
    [malt.response :as response]
    [metrics.meters :as meter]
    [malcolmx.core :as malx]
    [clojure.walk :refer [keywordize-keys]]))

(defn calc* [workbook-config
             {id :id params :params}
             & {profile? :profile? :or {profile? false}}]
  (if-let [result (session/with-locked-workbook workbook-config
                    (let [wb (:wb workbook-config)
                          in-sheet-name (:in_sheet_name workbook-config)
                          out-sheet-name (:out_sheet_name workbook-config)
                          str-data (mapv #(hash-map "id" (double (:id %))
                                                    "value" (:value %))
                                     params)]
                      (as-> wb $
                            (malx/update-sheet! $ in-sheet-name str-data :by "id")
                            (malx/get-sheet $ out-sheet-name :profile? profile?)
                            (mapv keywordize-keys $)
                            (assoc {:type :OUTCOMES} :data $))))]
    result
    {:type       :ERROR
     :error_type :INPROGRESS
     :error      (format "Workbook: %s calculation inprogress" id)}))

(defn calc [{storage :storage :as session-store}
            {id :id ssid :ssid params :params :as args}
            & {calc-profile :calc-profile}]
  (meter/mark! (:calls storage))
  (in-params/write! storage id params)
  (let [workbook-config (session/create-or-prolong session-store id ssid)
        rev (:rev workbook-config)]
    (cache/with-cache-by-key storage {:id id :rev rev :params params}
      (response/packet-init (calc* workbook-config args :profile? calc-profile)))))
