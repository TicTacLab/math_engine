(ns malt.formula-test
  (:use clojure.test
        clojure.tools.trace
        malt.math-parser.core
        [malt.math-parser.xls-read :as r]
        [malt.math-parser.math-xls :as m])
  (:require [malt.math-parser.xls-types :as xtypes])
  (:import [org.apache.poi.hssf.usermodel HSSFWorkbook]))

(defn roughly= [x y]
  (< (Math/abs (- x y))
     0.0001))

(defn create-cell []
  (let [wb (HSSFWorkbook.)
        sh (.createSheet wb "Test Sheet")
        rw (.createRow sh 0)]
    (.createCell rw 0)))

(defn eval-formula [cl formula]
  (.setCellFormula cl formula)
  (xtypes/extract cl))

(deftest normdist
  (let [cl (create-cell)]
    (is (roughly= (m/normal-distribution 96.6, 300, 11.45, false)
                  (eval-formula cl "NORMDIST(96.6,300,11.45,FALSE)")))))

(deftest poisson
  (let [cl (create-cell)]
    (is (roughly= (m/poisson-distribution 1, 2, false)
                  (eval-formula cl "POISSON(1,2,FALSE)")))))

(deftest critbinom
  (let [cl (create-cell)]
    (is (roughly= (m/binom-inv 96.6, 0.1, 0.2)
                  (eval-formula cl "CRITBINOM(96.6,0.1,0.2)")))))