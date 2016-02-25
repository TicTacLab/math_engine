(ns malt.storage.models
  (:require [malt.storage :refer [sql-exception-handler]]
            [yesql.core :refer [defqueries]]
            [dire.core :refer [with-handler!]])
  (:import (java.sql SQLException)
           (javax.xml.bind DatatypeConverter)))

(defn base64-decode [m ks]
  (reduce (fn [acc k]
            (update acc k #(DatatypeConverter/parseBase64Binary %)))
          m ks))

(defqueries "sql/files.sql")


;; ====== Public API

(defn get-raw-file [{spec :spec} id]
  (-> (get-raw-file* {:id id} {:connection spec})
      first
      (base64-decode [:file])))

(with-handler! #'get-raw-file
  SQLException
  sql-exception-handler)


(defn valid-model? [{spec :spec} id]
  (seq (valid-model*? {:id id} {:connection spec})))

(with-handler! #'valid-model?
  SQLException
  sql-exception-handler)