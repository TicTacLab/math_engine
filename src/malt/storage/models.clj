(ns malt.storage.models
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]])
  (:import (com.datastax.driver.core.utils Bytes)))

(defn get-model-file [storage id]
  (let [{:keys [conn]} storage]
    (some->
     (cql/select conn "models"
                 (columns :id :file :file_name :in_sheet_name :out_sheet_name)
                 (where [[= :id id]]))
     first
     (update-in [:file] #(Bytes/getArray %)))))
