(ns malt.math-parser.params
  (:require [clojure.tools.trace :refer [trace]]
            [malt.math-parser.xls-read
             :refer [get-cell set-cell cell-with-address sheet-evaluator]]
            [malt.math-parser.xls-types :as xtypes]))

(defn make-param
  [param]
  (let [value (:value param)
        exclude-value (dissoc param :id :value)
        
        params-map (assoc {}
                     :value (:value value)
                     :row (:row value)
                     :col (:col value)
                     :id (int (:value (:id param))))]
    (reduce #(assoc %1 (key %2) (:value (val %2))) params-map exclude-value)))

(defn parse-params
  "parse and calc outcomes from outcomes sheet OUT - by default"
  [sheet evaluator]
  (map make-param (sheet-evaluator sheet evaluator #(cell-with-address %))))

;;; functions for manipulate with config from core

(defn param-address [id params-records]
  ;;; extract [row col] of param from config
  (let [r (first (get params-records {:id id}))
        row (:row r) 
        col (:col r)]
    [row col]))

(defn get-params
  ;;; get value of param by id in Excel file
  [ids config]
  (let [params (:params config)
        sheet (:in-sheet config)
        address  (map #(let [[r c ] (param-address % params)]
                         (get-cell sheet r c))ids)]
    (map xtypes/extract address)))

(defn set-param 
  ;;; set value of cell new is map with address of param cell
  [config new]
  (let [id (:id new)
        value (:value new)
        params (:params config)
        sheet (:in-sheet config)
        r (first (get params {:id id}))
        row (:row r) 
        col (:col r)]
    (if (or (nil? row ) (nil? col))
      (do (throw (Exception. (str "No such param id: " id)))
          config)
      (do 
        (set-cell sheet row col value)
        (assoc config :in-sheet sheet)))))

;(def p {:params [{:id 10, :value 2} {:id 11, :value 3}], :id 1})
(defn set-params
  "take map of params {id:value}"
  [params config]
  (reduce #(set-param %1 %2) config params))
