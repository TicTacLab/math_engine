(ns malt.cache
  (:require [clojure.string :refer [join]]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [metrics.meters :as meter]
            [schema.core :as s]
            [digest :refer [md5]]
            [cheshire.core :as json]
            [zabbix-clojure-agent.gauges :as agauge]
            [metrics.core :as metrics])
  (:import [com.codahale.metrics RatioGauge$Ratio]
           [net.spy.memcached AddrUtil]
           [net.spy.memcached MemcachedClient
                              ConnectionFactoryBuilder
                              FailureMode ConnectionFactoryBuilder$Protocol]))

(defn connect [servers reconnection-delay op-timeout]
  (let [fb (doto (ConnectionFactoryBuilder.)
             (.setFailureMode FailureMode/Redistribute)
             (.setMaxReconnectDelay reconnection-delay)     ;; seconds
             (.setOpTimeout op-timeout)                     ;; ms, timeout on get op
             (.setOpQueueMaxBlockTime 50)                   ;; ms, await for offering set op
             (.setProtocol ConnectionFactoryBuilder$Protocol/BINARY))]
    (def fac (.build fb))
    (MemcachedClient. (.build fb)
                      (AddrUtil/getAddresses (join " " servers)))))

(defn gen-cache-key [id rev params]
  (let [key-params (->> params
                        (sort-by :id)
                        (map :value)
                        (join ",")
                        (md5)
                        (doall))]
    (format "model:%s:%s:%s" id rev key-params)))

(defn put [{conn :conn} {model-id :id rev :rev params :params} value]
  (try
    (.set conn (gen-cache-key model-id rev params) 0 (json/generate-string value))
    (catch Exception e
      (log/error e "Exception during cache put"))))

(defn fetch [{conn :conn} {model-id :id rev :rev params :params}]
  (try
    (when-let [result (.get conn (gen-cache-key model-id rev params))]
      (json/decode result))
    (catch Exception e
      (log/error e "Exception during cache fetching")
      nil)))

(defmacro with-cache-by-key [cache key & body]
  `(if (:cache-on ~cache)
     (if-let [result# (fetch ~cache ~key)]
       (do
         (meter/mark! (:hits ~cache))
         result#)
       (when-let [result# (do ~@body)]
         (put ~cache ~key result#)
         result#))
     (do ~@body)))

(defn disconnect [conn]
  (.shutdown conn))

(defrecord Storage [conn
                    cache-nodes
                    cache-op-timeout
                    cache-reconnection-delay
                    cache-on
                    cache-hit
                    hits
                    calls]
  component/Lifecycle

  (start [component]
    (let [conn (connect cache-nodes cache-reconnection-delay cache-op-timeout)
          hits (meter/meter ["math_engine" "hits"])
          calls (meter/meter ["math_engine" "calls_rate"])
          cache-hit (agauge/ratio-gauge-fn ["malt_engine" "cache_hit"]
                                           #(RatioGauge$Ratio/of (meter/rate-one hits)
                                                                 (meter/rate-one calls)))]
      (log/info "Cache started")
      (assoc component
        :conn conn
        :hits hits
        :calls calls
        :cache-hit cache-hit)))

  (stop [component]
    (when conn
      (disconnect conn))
    (when hits
      (metrics/remove-metric "hits"))
    (when calls
      (metrics/remove-metric "calls"))
    (when cache-hit
      (metrics/remove-metric ["malt_engine" "cache_hit"]))
    (log/info "Cache stopped")
    (assoc component
      :conn nil
      :hits nil
      :calls nil
      :cache-hit nil)))


(def CacheSchema
  {:cache-nodes      [s/Str]
   :cache-op-timeout s/Int
   :cache-reconnection-delay s/Int
   :cache-on         s/Bool})

(defn new-cache [m]
  (as-> m $
        (select-keys $ (keys CacheSchema))
        (s/validate CacheSchema $)
        (map->Storage $)))





