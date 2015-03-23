(ns malt.multi.calc_async
  (:use [clojure.tools.trace]
        [malt.utils :only [join-tail]]
        [malt.math_parser.xls_types :as xtypes :only (extract)]
        [clojure.tools.logging :only (info)])
  (:require [malt.global :refer (malt-config)]
            [malt.multi.worker :as w :refer (workers-free?)]
            [malt.math_parser.sessions :refer (sessions)]))

(set! *warn-on-reflection* true)

;; woekrbooks map with {wb-id [wb-chunks wb-calc-timeout]}
(def workbooks-stats (agent {}))

(defn
  ^clojure.lang.PersistentHashMap
  workbook-add
  [^clojure.lang.PersistentHashMap chs
   ^clojure.lang.PersistentArrayMap {wb-id :wb-id wb-chunk :wb-chunk calc-time :calc-time}]
  (assoc chs wb-id {:chunks  wb-chunk :time calc-time}))

(defn workbooks-stats-get
  []
  (let [total-sessions (->> @sessions
                            vals
                            (map :file-name)
                            (map #(list % 1))
                            (reduce (fn [acc [f s]]
                                      (let [current-session
                                            (-> acc (get f) (get :sessions 0))]
                                        (assoc acc f {:sessions (+ s current-session)}))) {}))]
    (merge-with merge  @workbooks-stats total-sessions)))

(defn #^clojure.lang.PersistentList
  extract-one-thread
  ;; #^WorkbookConfig
  [wb-config]
  (let [{cells :out-cells
         evaluator-fn :evaluator-fn} wb-config
         evaluator (evaluator-fn)]
;;    (logger/info (Thread/currentThread) evaluator)
    (->> cells (map #(doall (xtypes/extract % evaluator))) doall)))

(defn chunking-jobs
  #^clojure.lang.PersistentList
  [^Integer num-of-chunks ^clojure.lang.PersistentList cells]
  (if (and (integer? num-of-chunks) (> num-of-chunks 1))
    (let [cells-count (count cells)
          num-jobs-per-worker (quot cells-count num-of-chunks)
          rm (rem cells-count num-of-chunks)]
      (->>
       (partition num-jobs-per-worker num-jobs-per-worker nil cells)
       (join-tail rm)))
    cells))

(defn #^clojure.lang.PersistentList
  jobs-exec
  [wb-config]
  (let [{cells :out-cells
         workbook :wb
         wb-id :id
         evaluator-fn :evaluator-fn
         num-of-chunks :threads
         ssid :ssid} wb-config
        ]
    (if (w/workers-free?)
      ;; else calc in thread pool
      (->>
       (chunking-jobs num-of-chunks cells)
       (map #(list
              (assoc {}
                :evaluator-fn evaluator-fn
                :out-cells %)))
       (w/call ssid extract-one-thread) ;; call thread
       (mapcat concat)
       )
      ;; then calc in same thread
      (extract-one-thread wb-config)
      )))

(defn #^clojure.lang.PersistentList
  ;; #^WorkbookConfig
  schedule-extract
  [wb-config]
  (let [begin (System/currentTimeMillis)
        {cells :out-cells
         wb-id :file-name
         num-of-chunks :threads 
         } wb-config
           result (if (<= num-of-chunks 1)
                    (extract-one-thread wb-config)
                    (jobs-exec wb-config))
        end (- (System/currentTimeMillis) begin)
        ]
    (send workbooks-stats workbook-add {:wb-id wb-id :wb-chunk num-of-chunks :calc-time end})
    result
    ))
