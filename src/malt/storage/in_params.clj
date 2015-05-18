(ns malt.storage.in-params
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [taoensso.nippy :as nippy]))

(defn write! [{conn :conn} model-id in-params]
  (cql/insert conn "in_params"
                    {:model_id model-id
                     :params   (nippy/freeze in-params)}))