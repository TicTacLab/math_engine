(ns malt.session
  (:require
    [com.stuartsierra.component :as component]
    [schema.core :as s]
    [clojure.core.cache :as cache]
    [malt.storage.models :as models]
    [metrics.gauges :as gauge]
    [metrics.core :as metrics]
    [malcolmx.core :as malx])
  (:import [clojure.lang PersistentArrayMap]
           [java.util.concurrent Semaphore]
           (org.apache.poi.ss.usermodel Workbook)))

(defrecord WorkbookConfig [id rev wb file_name file ssid lock in_sheet_name out_sheet_name])

(defn #^WorkbookConfig config-to-workbook [#^PersistentArrayMap config]
  (map->WorkbookConfig (assoc config
                         :lock (Semaphore. 1)
                         :wb (-> config
                                 :file
                                 (malx/parse)))))

(defmacro with-locked-workbook [workbook-config & body]
  `(when (.tryAcquire ^Semaphore (:lock ~workbook-config))
     (try
       ~@body
       (finally (.release ^Semaphore (:lock ~workbook-config))))))

(defn fetch [session-store ssid]
  (-> (get session-store :session-table)
      deref
      (cache/lookup ssid)))

(defn prolong! [session-store ssid session]
  (swap! (get session-store :session-table) assoc ssid session)
  session)

(defn create! [session-store id ssid]
  (-> (:session-table session-store)
      (swap! (fn [table]
               (if (cache/has? table ssid)
                 table
                 (cache/miss table ssid
                             (-> session-store
                                 :storage
                                 (models/get-model id)
                                 config-to-workbook
                                 (assoc :ssid ssid))))))
      (get ssid)))

(defn delete! [session-store ssid]
  (let [workbook-config (fetch session-store ssid)]
    (swap! (:session-table session-store) cache/evict ssid)
    (try (.close ^Workbook (:wb workbook-config))
         (catch Exception _))
    true))

(defn create-or-prolong [session-store id ssid]
  (if-let [session (fetch session-store ssid)]
    (prolong! session-store ssid session)
    (create! session-store id ssid)))

(defrecord SessionStore
    [session-table session-ttl storage sessions-count]
  ;; session-ttl in seconds
  component/Lifecycle
  (start [component]
    (let [session-table (atom (cache/ttl-cache-factory {} :ttl (* 1000 session-ttl)))]
      (assoc component
        :session-table session-table
        :sessions-count (gauge/gauge-fn ["malt_engine" "sessions_count"]
                                         #(double (count @session-table))))))

  (stop  [component]
    (when sessions-count (metrics/remove-metric ["malt_engine" "sessions_count"]))
    (assoc component
      :session-table nil
      :sessions-count nil)))

(def SessionStoreSchema
  {:session-ttl s/Int})

(defn new-session-store [m]
  (as-> m $
        (select-keys $ (keys SessionStoreSchema))
        (s/validate SessionStoreSchema $)
        (map->SessionStore $)))
