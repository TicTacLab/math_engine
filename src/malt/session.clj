(ns malt.session
  (:require
    [com.stuartsierra.component :as component]
    [clojure.tools.trace :refer (trace)]
    [schema.core :as s]
    [clojure.core.cache :as cache]
    [malt.storage.models :as models]
    [malt.math-parser.xls-types :as xtypes]
    [malt.math-parser.xls-read :refer
     (read-workbook write-workbook map-workbook make-formula-evaluator extract-eval parse-head blank-row?)]
    [malt.math-parser.params :refer (parse-params set-params get-params)]
    [clojure.set :refer (index)]
    [metrics.gauges :as gauge]
    [metrics.core :as metrics])
  (:import [clojure.lang PersistentArrayMap]
           [java.util.concurrent Semaphore]))

(defrecord WorkbookConfig
  [id wb evaluator params sheets
   in-sheet out-sheet file-name
   out-cells
   out-header
   ssid lock])

(defn #^WorkbookConfig config-to-workbook [#^PersistentArrayMap config]
  (let [{:keys [file in_sheet_name out_sheet_name]} config
        lock (Semaphore. 1)
        wb (read-workbook file)
        evaluator (make-formula-evaluator wb)
        sheets (map-workbook wb)
        in-sheet (sheets in_sheet_name)
        out-sheet (doall  (sheets out_sheet_name))
        params (index (parse-params in-sheet evaluator) [:id])
        outcomes-cells (->> out-sheet xtypes/extract (remove blank-row?))
        header (parse-head outcomes-cells evaluator)
        eval-header {:evaluator evaluator :out-header header}
        out-cells (->> outcomes-cells rest (mapv #(assoc eval-header :cell %)))
        ]
    (map->WorkbookConfig (assoc config
                           :lock lock
                           :wb wb
                           :in-sheet in-sheet
                           :out-sheet out-sheet
                           :sheets sheets
                           :params params
                           :evaluator evaluator
                           :out-cells out-cells
                           :out-header header))))

(defmacro with-locked-workbook [workbook-config & body]
  `(when (.tryAcquire (:lock ~workbook-config))
     (try
       ~@body
       (finally (.release (:lock ~workbook-config))))))

(defn fetch [session-store ssid]
  (-> (get session-store :session-table)
      deref
      (cache/lookup ssid)))

(defn save! [session-store ssid v]
  ;; check locking session before save, or return old state 
  (swap! (get session-store :session-table) assoc ssid v)
  v)

(defn create! [session-store id ssid]
  (-> (:session-table session-store)
      (swap! (fn [table]
               (if (cache/has? table ssid)
                 table
                 (assoc table ssid
                              (-> session-store
                                  :storage
                                  (models/get-model-file id)
                                  config-to-workbook
                                  (assoc :ssid ssid))))))
      (get ssid)))

(defn create-if-not-exists [session-store id ssid]
  (if-let [session (fetch session-store ssid)]
    session
    (create! session-store id ssid)))

(defn prolong! [session-store ssid]
  (save! session-store ssid (fetch session-store ssid)))

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

(def SessionStoreConfig
  {:session-ttl s/Int})

(defn new-session-store [m]
  (when (s/validate SessionStoreConfig m)
    (map->SessionStore m)))
