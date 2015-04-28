(ns malt.math-parser.outcome
  (:use
   [malt.math-parser.xls-read :only (sheet-evaluator)]))

(defn parse-outcomes
  "parse and calc outcomes from outcomes sheet OUT - by default"
  [sheet evaluator]
  (sheet-evaluator sheet evaluator))

(defn get-outcomes
  "get outcome by id"
  [ids config]
  (->> (:out-sheet config)
       parse-outcomes
       (filter #(some (fn [x] (= (:id %) x)) ids))))
