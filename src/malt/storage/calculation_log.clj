(ns malt.storage.calculation-log
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [cheshire.core :as json]))

(defn write! [{conn :conn} session-id model-id in-params out-params]
  (cql/insert conn "calculation_log" {:session_id session-id
                                      :model_id   model-id
                                      :in_params  (json/generate-string in-params)
                                      :out_params out-params}))
