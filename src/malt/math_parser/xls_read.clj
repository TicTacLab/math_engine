(ns malt.math-parser.xls-read
  (:require
    [clojure.java.io :refer [file input-stream]]
    [malt.math-parser.xls-types :as xtypes]
    [malt.utils :as utils])
  (:import
    (org.apache.poi.ss.usermodel WorkbookFactory Workbook Cell Sheet Row)
    (java.io FileOutputStream InputStream)
    (org.apache.poi.ss.util CellReference)
    [org.apache.poi.xssf.usermodel XSSFCell]))

(def udfs [xtypes/normdist-udf
           xtypes/pascal-udf
           xtypes/binomial-udf
           xtypes/binom-inv-udf
           ])

(defn udfs-add [^Workbook wb udfs]
  (.addToolPack wb udfs))

(defn udfs-wb [wb]
  (doseq  [f udfs] (udfs-add wb f)))

(defn make-formula-evaluator [^Workbook wb]
  (udfs-wb wb)
  (.. wb getCreationHelper createFormulaEvaluator))

(defn workbook
  "Create or open new excel workbook. Defaults to xlsx format."
  [^InputStream input]
  (WorkbookFactory/create input))

(defn sheets
  "Get seq of sheets."
  [^Workbook wb] (map #(.getSheetAt wb %1) (range 0 (.getNumberOfSheets wb))))

(defn rows
  "Return rows from sheet as seq.  Simple seq cast via Iterable implementation."
  [sheet] (seq sheet))

(defn cells
  "Return seq of cells from row.  Simpel seq cast via Iterable implementation." 
  [row] (seq row))

(defn get-address
  [^Cell cell]
  {:row (.getRowIndex cell) :col (.getColumnIndex cell)})

;; params

(defn parse-head [rows evaluator]
  (->> rows
       first
       (map #(xtypes/extract % evaluator))
       (map keyword)))

(defn cell-with-address
  "return vals in vector with [row col value]"
  [cell]
  (assoc (get-address cell)
    :value (xtypes/extract cell)))

(defn values-to-map
  "return calced cells"
  [head cells]
  (apply assoc {} (interleave head cells)))

(defn sheet-evaluator
  "parse sheet and apply fn to every row"
  ([sheet evaluator]
     (sheet-evaluator sheet evaluator #(xtypes/extract % evaluator)))
  ([sheet evaluator fn]
     (let [rs (xtypes/extract sheet)
           head (parse-head rs evaluator)]
       (->> rs
            rest
            (map xtypes/extract)
            (map  #(map fn %))
            (map #(values-to-map head %))))))

(defn extract-to-hash [{:keys [evaluator out-header cell]}]
  (zipmap out-header (xtypes/extract cell evaluator)))

(defn extract-with-profile [calc-profile args]
  (if calc-profile
    (utils/with-timer (extract-to-hash args))
    (extract-to-hash args)))

(defn extract-eval
  ;; #^WorkbookConfig
  [wb-config calc-profile]
  (let [{:keys [evaluator out-cells]} wb-config
        result (mapv #(extract-with-profile calc-profile (assoc % :evaluator evaluator)) out-cells)
        outcomes-total (count out-cells)
        outcomes-calced (count result)]
    (if (= outcomes-total outcomes-calced)
      (assoc {} :type :OUTCOMES :data result)
      (assoc {}
        :type :ERROR
        :error_type :CALCFALSE
        :error (str "Workbook: " (:id wb-config) ". Outcomes: " outcomes-calced " of " outcomes-total)))))

(defn read-workbook [bytes]
  (with-open [in (input-stream bytes)]
    (workbook in)))

(defn write-workbook [^Workbook wb ^String file-name]
  (with-open [out (FileOutputStream. file-name)]
    (.write wb out)))

(defn blank-row? [^Row row]
  (->> (iterator-seq (.cellIterator row))
       (map #(.getCellType ^Cell %))
       (every? #(= XSSFCell/CELL_TYPE_BLANK %))))

(defn map-workbook
  "Lazy workbook report."
  [wb]
  (zipmap (map #(.getSheetName ^Sheet %) (sheets wb)) (sheets wb)))

(defn get-cell
  "Sell cell within row"
  ([^Row row col] (.getCell row col))
  ([^Sheet sheet row col] (get-cell (or (.getRow sheet row) (.createRow sheet row)) col)))

(defn set-cell
  "set cell value"
  [sheet row col value]
  (let [cell (get-cell sheet row col)]
    (xtypes/pack cell value)
    cell))