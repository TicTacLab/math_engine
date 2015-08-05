(ns malt.storage.models
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns limit]])
  (:import (com.datastax.driver.core.utils Bytes)))

(defn get-model [storage id]
  (let [{:keys [conn]} storage]
    (-> (cql/get-one conn "models"
                     (columns :id :rev :file :file_name :in_sheet_name :out_sheet_name)
                     (where [[= :id id]]))
        (update-in [:file] #(Bytes/getArray %)))))


(defn valid-model? [storage id]
  (let [{:keys [conn]} storage]
    (seq (cql/select conn "models"
                     (columns :id)
                     (where [[= :id id]])
                     (limit 1)))))