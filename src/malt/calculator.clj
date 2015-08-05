(ns malt.calculator
  (:require
    [malt.session :as session]
    [malt.storage
     [cache :as cache]
     [in-params :as in-params]]
    [malt.response :as response]
    [metrics.meters :as meter]
    [malcolmx.core :as malx]
    [clojure.walk :refer [keywordize-keys]]))

(defn non-empty-outcome [outcome]
  (:id outcome))

(defn calc* [workbook-config
             {params :params}
             & {profile? :profile? :or {profile? false}}]
  (session/with-locked-workbook workbook-config
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
                                        (filter non-empty-outcome $)))))

(defn calc [{storage :storage :as session-store}
            {id :model_id ssid :event_id params :params :as args}
            & {profile? :profile?}]
  (meter/mark! (:calls storage))
  (in-params/write! storage id params)
  (let [workbook-config (session/create-or-prolong session-store id ssid)
        rev (:rev workbook-config)]
    (cache/with-cache-by-key storage {:id id :rev rev :params params}
      (calc* workbook-config args :profile? profile?))))
