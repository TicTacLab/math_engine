(ns malt.calculator
  (:require
    [malt.session :as session]
    [malt.storage
     [cache :as cache]
     [in-params :as in-params]]
    [metrics.meters :as meter]
    [malcolmx.core :as malx]
    [clojure.walk :refer [keywordize-keys]]))

(defn non-empty-outcome [outcome]
  (:id outcome))

(defn calc* [workbook-config
             params
             profile?]
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
            model-id
            event-id
            params
            profile?]
  (meter/mark! (:calls storage))
  (in-params/write! storage model-id params)
  (let [workbook-config (session/create-or-prolong session-store model-id event-id)
        rev (:rev workbook-config)]
    (cache/with-cache-by-key storage {:id model-id :rev rev :params params}
      (calc* workbook-config params profile?))))
