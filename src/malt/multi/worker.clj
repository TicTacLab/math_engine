(ns malt.multi.worker
  (:require [malt.global :refer (malt-config)]
            [malt.utils :refer (get-timestamp)]
            [clojure.tools.logging :refer (error info) :as logger]
            [clojure.tools.trace :refer (trace)]
            [clojure.set :as set :refer (index)])
  (:import [java.util.concurrent
            Executors
            ThreadPoolExecutor
            TimeUnit
            CancellationException
            FutureTask]))
(set! *warn-on-reflection* true)

;; SET of active workers threads
(def workers (ref #{}))   ;; workers with states
(def workers-by-ssid (ref {}))

(defn scheduler-config [] (->> @malt-config :scheduler))
(defn expire-calc-time [] (-> (scheduler-config) (get :calc-timeout 500)))
(defn workers-free? [] (< (count @workers) (-> (scheduler-config) (get :workers 100))))

(add-watch workers :sort-by-ssid (fn [c k os ns]
                                   (dosync (ref-set workers-by-ssid (set/index ns [:ssid])))))

;;(add-watch workers-by-ssid nil (fn [c k os ns] (trace "threads in progress: " (keys ns))))


(defn #^clojure.lang.PersistentHashSet
  worker-add-state
  [w]
  (dosync (alter workers conj w)))
(defn #^clojure.lang.PersistentHashSet
  worker-del-state
  [w]
  (dosync (alter workers disj w)))
(defn #^clojure.lang.PersistentHashSet
  worker-update-state
  [w1 w2]
  (dosync (alter workers disj w1)
          (alter workers conj w2)))
(defn #^Boolean worker-exists?
  [w]
  (contains? @workers w))

(defn exec
  [func data]
;;  (trace (Thread/currentThread))
  (try
    (apply func data)
    (catch Exception e
      (logger/error (str "caught exception: " (.getMessage e))))))

(defn run-main [^ThreadPoolExecutor pool tasks]
  (try
    (doseq [future (.invokeAll pool tasks (expire-calc-time) TimeUnit/MILLISECONDS)]
      (.get ^FutureTask future))
    (catch CancellationException e
      (logger/error "main thread timeout"))))

(defprotocol WorkerProtocol
  (stop  [this] "stop workers")
  (terminate  [this] "terminate all workers")
  (closed?  [this] "if thread pool was terminated")
  (call  [this func data] "eval func")
  (expire? [this] "expired calc?")
  (expire! [this] "if expired? del thread calc")
  (get-result [this] "get result"))

(defrecord WorkerThread
    [ssid timestamp ^ThreadPoolExecutor thread result]
  WorkerProtocol
  
  (call
    [this func data]
    (let [count-tasks (count data)
          refs (atom [])
          thread-pool (Executors/newFixedThreadPool count-tasks)
          tasks (map #(fn []
                        (swap! refs conj (exec func %))) data)
          this-th (assoc this :timestamp (get-timestamp) :thread thread-pool :result refs)
          ]
      (worker-add-state this-th)
      (run-main thread-pool tasks)
      (get-result this-th)
      ))
  
  (stop [this]
    (.shutdown ^ThreadPoolExecutor thread)
    (worker-del-state this)
    true)

  (terminate [this]
    (.shutdownNow ^ThreadPoolExecutor thread)
    (worker-del-state this)
    true)

  (closed? [this]
    (or  (.isTerminated ^ThreadPoolExecutor thread)
         (.isTerminating ^ThreadPoolExecutor thread)
         (.isShutdown ^ThreadPoolExecutor thread)))
  
  (expire? [this]
    (> (- (get-timestamp) timestamp) (expire-calc-time)))

  (expire! [this]
    (if (expire? this)
      (terminate this)
      false))
  
  (get-result [this]
    (if-not (closed? this)
      (let [r @result]
        (stop this)
        r))))

(extend-protocol WorkerProtocol
  String ;; set SSID to worker thread
  (call [this func data] (-> (->WorkerThread this nil nil nil) (call func data))))

(defn stop-by-ssid [^String ssid]
  (if-let [threads (get @workers-by-ssid {:ssid  ssid})]
    (doseq [t threads] (terminate t))))

(comment
  ;; gc
  (defn kill-expired-workers
    []
    (let [w (count @workers)]
      (if (< 1 w) (logger/info "total workers in progress: " (count @workers))))
    (doseq [w @workers] (expire! w))))


(defn tfn
  ([] (tfn 1 30000))
  ([i t]
     (trace (Thread/currentThread))
     (Thread/sleep t)
     (trace (* i 10))))
