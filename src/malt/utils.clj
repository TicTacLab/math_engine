(ns malt.utils
  (:import [java.io PushbackReader])
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as logger])
  (:use    [clojure.tools.trace :only (trace)]))


(def not-nil? (complement nil?))


(def max-eval-time 10.0)

(defmacro utry [error-fn & body]
  `(try ~@body
        (catch Exception e#
          (logger/error (throw (Exception. (str ~error-fn "\n\n" (.getMessage  e#) )))))))

(defn round [x]
  (-> (* x 100) (Math/round) double  (/ 100 )))

(defn string-to-double [x]
  (cond
    (float? x) x
    (string? x) (Double/parseDouble x)
    (number? x) (double x)))

(defn string-to-integer [x]
  (if (integer? x)
    x
    (Integer/parseInt x)))

(defmulti trims type)
(defmethod trims java.lang.String [x] (trims (string-to-double x)))
(defmethod trims java.lang.Long [x]  (trims (double x)))
(defmethod trims java.lang.Byte [x] x)
(defmethod trims java.lang.Double [x]
  (round x)) ;(format "%,2f" x)))
(defmethod trims :default [x]
  (println "error of value for trim " x (type x)))

(defn trim-equal [x y]
  (= (trims x) (trims y)))

(defn read-config
  [path]
  "read config from file-path"
  (with-open [r (io/reader path)]
    (read (PushbackReader. r))))

(defn timer [func str-format]
  (let [start (. System (nanoTime))
        ret (func)
        time-call
        (/
         (double 
          (- (. System (nanoTime)) start))
         1000000.0)]
    (if (> time-call max-eval-time)
      (trace time-call str-format))
    ret))

(defn #^clojure.lang.PersistentList
  drop-last-element
  [cells]
  [(last cells) (drop-last cells)])

(defn #^clojure.lang.PersistentList
  join-tail
  [^Integer remain ^clojure.lang.PersistentList cells]
  (if (= 0 remain) cells
      (let [[l1 c1] (drop-last-element cells)
            [l2 c2] (drop-last-element c1)
            l3  (concat l2 l1)
            ] (conj c2 l3))))
(defn mfn [f]
  (let [mem (atom {})]
    (fn [& args]
      (if-let [e (find @mem args)]
        (val e)
        (let [ret (apply f args)]
          (swap! mem assoc args ret)
          ret)))))

(defn ^Integer
  get-timestamp
  []
  (System/currentTimeMillis))

(defn with-timer [func & args]
  (let [start (. System (nanoTime))
        ret (apply func args)
        time-call
        (/
         (double 
          (- (. System (nanoTime)) start))
         1000000.0)]
    (assoc ret :timer (int time-call))))

