(ns malt.math_parser.xls_types
  (:use [malt.math_parser.math_xls :only [normal-distribution
                                          pascal-distribution
                                          poisson-distribution
                                          binomial-distribution
                                          poissson-replace-bool-int
                                          norm-dist-replace
                                          sum-replace-long
                                          binom-inv]]
        [malt.utils :only [utry timer]]
        [clojure.tools.logging :as logger :only (info error)]
        [clojure.tools.trace :only (trace)] 
        )
  (:import
   (org.apache.poi.ss.formula.udf AggregatingUDFFinder DefaultUDFFinder)
   (org.apache.poi.ss.formula.functions Fixed4ArgFunction Fixed3ArgFunction FreeRefFunction)
   (org.apache.poi.ss.formula WorkbookEvaluator)
   (org.apache.poi.ss.formula.eval NumberEval BoolEval RefEvalBase ErrorEval StringEval OperandResolver)
   (org.apache.poi.ss.usermodel Cell CellValue)
   (org.apache.poi.hssf.usermodel HSSFCell HSSFRow HSSFSheet)
   (org.apache.poi.xssf.usermodel XSSFCell XSSFRow XSSFSheet)))

(defprotocol CastClass
  (extract [this] [this value] "calc formulas and extract data from cells")
  (pack [this] [this value])
  (toString [this])
  (toMap [this] )
  (formulaScan [this] "scan formulas and replace to correct version")
  (isFormula? [this] "boolean predicat for formula type of cell"))


(defn error-code [code]
  (condp = code
    15 "VALUE!"
    7  "DIV/0"
    (str "formula error:" code )))

(defn formula-eval-error [cell]
  (str " cant eval formula:" (.getCellFormula cell)
       " sheet:" (.. cell getSheet getSheetName)
       " row:" (.getRowIndex cell)
       " cell:"  (.getColumnIndex cell)))


(defn formula-eval
  "eval excel formulas"
  ([cell]
     (->>
      (.. cell getSheet getWorkbook getCreationHelper createFormulaEvaluator)
      (formula-eval cell)
      ))
  ([cell evaluator]
     (try
       (.evaluate evaluator cell)
       (catch Exception e
         (logger/error (.getMessage e) (formula-eval-error cell))))))
  

(extend-protocol CastClass
   
   nil
   (extract ([this] this) ([this evaluator] this) ([this evaluator output-channel] this))
   (pach [this] this)

   ErrorEval
   (extract [this]  {:error (.getErrorCode this)})
   
   NumberEval
   (extract [this] (.getNumberValue this))
   
   BoolEval
   (extract [this] (.getBooleanValue this))
   
   RefEvalBase
   (extract [this]
     (extract (.getInnerValueEval this)))
      
   HSSFSheet
   (extract [this]  (seq this))

   XSSFSheet
   (extract [this]  (seq this))
   
   HSSFRow
   (formulaScan [this] (seq this))
   (extract
     ([this] (seq this))
     ([this evaluator] (map #(extract % evaluator) (seq this))))
   
   XSSFRow
   (formulaScan [this] (map formulaScan (seq this)))
   (extract
     ([this] (seq this))
     ([this evaluator] (map #(extract % evaluator) (seq this))))
   
   HSSFCell
   (isFormula? [this]
     (= Cell/CELL_TYPE_FORMULA (.getCellType this)))

   (formulaScan [this]
     (if  (isFormula? this)
       (.setCellFormula this (poissson-replace-bool-int (.getCellFormula this)))
       this))
   
   (extract
     ([this]
        (condp = (.getCellType this)
          Cell/CELL_TYPE_FORMULA    (extract (formula-eval this))
          Cell/CELL_TYPE_NUMERIC    (.getNumericCellValue this)
          Cell/CELL_TYPE_STRING     (.getStringCellValue this)
          Cell/CELL_TYPE_BOOLEAN    (.getBooleanCellValue this)
          Cell/CELL_TYPE_ERROR      (error-code (.getErrorCellValue this))
          Cell/CELL_TYPE_BLANK      nil
          (logger/error 
           (str "undef cell type: " (.getCellType this)))))
     ([this evaluator]
        (condp = (.getCellType this)
          Cell/CELL_TYPE_FORMULA    (extract (formula-eval this evaluator))
          Cell/CELL_TYPE_NUMERIC    (.getNumericCellValue this)
          Cell/CELL_TYPE_STRING     (.getStringCellValue this)
          Cell/CELL_TYPE_BOOLEAN    (.getBooleanCellValue this)
          Cell/CELL_TYPE_ERROR      (error-code (.getErrorCellValue this))
          Cell/CELL_TYPE_BLANK      nil
          (logger/error 
           (str "undef cell type: " (.getCellType this))))))
   (pack [this value] (->> (pack value) (.setCellValue this)))

   XSSFCell
   (isFormula? [this]
     (= Cell/CELL_TYPE_FORMULA (.getCellType this)))
   
   (formulaScan [this]
     (if  (isFormula? this)
           (.setCellFormula this (sum-replace-long (.getCellFormula this)))
;           (.setCellFormula this (poissson-replace-bool-int (.getCellFormula this)))
           this))

   
   (extract
     ([this]
        (condp = (.getCellType this)
          Cell/CELL_TYPE_FORMULA    (extract (formula-eval this))
          Cell/CELL_TYPE_NUMERIC    (.getNumericCellValue this)
          Cell/CELL_TYPE_STRING     (.getStringCellValue this)
          Cell/CELL_TYPE_BOOLEAN    (.getBooleanCellValue this)
          Cell/CELL_TYPE_ERROR      (error-code (.getErrorCellValue this))
          Cell/CELL_TYPE_BLANK      nil
          (logger/error 
           (str "undef cell type: " (.getCellType this)))))
     ([this evaluator]
        (condp = (.getCellType this)
          Cell/CELL_TYPE_FORMULA     (extract (formula-eval this evaluator))
          Cell/CELL_TYPE_NUMERIC     (.getNumericCellValue this)
          Cell/CELL_TYPE_STRING      (.getStringCellValue this)
          Cell/CELL_TYPE_BOOLEAN     (.getBooleanCellValue this)
          Cell/CELL_TYPE_ERROR       (error-code (.getErrorCellValue this))
          Cell/CELL_TYPE_BLANK      nil
          (logger/error 
           (str "undef cell type: " (.getCellType this))))))
   (pack [this value] (->> (pack value) (.setCellValue this)))
   
   
   Cell
   (extract [this]
     (condp = (.getCellType this)
       Cell/CELL_TYPE_FORMULA    (extract (formula-eval this))
       Cell/CELL_TYPE_NUMERIC    (.getNumericCellValue this)
       Cell/CELL_TYPE_STRING     (.getStringCellValue this)
       Cell/CELL_TYPE_BOOLEAN    (.getBooleanCellValue this)
       Cell/CELL_TYPE_ERROR      (error-code (.getErrorCellValue this))
       Cell/CELL_TYPE_BLANK      nil
       (logger/error 
        (str "undef cell type: " (.getCellType this)))))
   (pack [this value] (.setCellValue this value))
   
   CellValue
   (extract [this]
     (condp = (.getCellType this)
       Cell/CELL_TYPE_NUMERIC    (.getNumberValue this)
       Cell/CELL_TYPE_STRING     (.getStringValue this)
       Cell/CELL_TYPE_BOOLEAN    (.getBooleanValue this)
       Cell/CELL_TYPE_ERROR      (error-code (.getErrorValue this))
       (logger/error 
        (str "undef cell type: " (.getCellType this)))))
   
   java.lang.Double
   (pack [this] this)
   
   java.lang.Long
   (pack [this] (double this))
   
   java.lang.String
   (pack [this] this)
   (extract [this] this))

(defn register-fun! [nm fun]
  (when-not (contains? (WorkbookEvaluator/getSupportedFunctionNames) nm)
    (WorkbookEvaluator/registerFunction nm fun)))

;;; NORMDIST impementation fun

(def normdist
  (proxy [Fixed4ArgFunction] []
    (evaluate [col-index  row-index x mean standard_dev cumulative]
      (let [cumulative-value (extract cumulative)
            standard_dev-value (extract standard_dev)
            x-value (extract  x)
            mean-value (extract mean)
            ]
;		(println [standard_dev-value x-value mean-value])
		(if (every? #(or (integer? %) (float? %)) [standard_dev-value x-value mean-value])
		  (NumberEval. (normal-distribution x-value mean-value standard_dev-value cumulative-value))
		  (StringEval. (str [x-value mean-value standard_dev-value]))
		  )))))

(defn extract-operand [args n row col]
  (-> (nth args n) (OperandResolver/getSingleValue  row col) extract))



;; install normdist 
(register-fun! "NORMDIST" normdist)



;;; POISSON DISTRIBUTION

(def poisson
  (proxy [Fixed3ArgFunction] []
    (evaluate [col-index  row-index x mean cumulative]
      (let [cumulative-value (extract cumulative)
            x-value (extract  x)
            mean-value (extract mean)
            ]
		(if (every? #(and (or (integer? %) (float? %)) (>= % 0)) [x-value mean-value])
		  (NumberEval. (poisson-distribution x-value mean-value cumulative-value))
		  (StringEval. (str [x-value mean-value ]))
		  )))))

(register-fun! "POISSON" poisson)


;;; NORM.DIST function implementation.

(def normdist-free
  (reify FreeRefFunction
    (evaluate [this args ec]
      (let [row  (.getRowIndex ec)
            col  (.getColumnIndex ec)
            x (extract-operand args 0 row col)
            mean (extract-operand args 1 row col)
            standard_dev (extract-operand args 2 row col)
            cumulative (extract-operand args 3 row col)
            ]
		(if (every? #(or (integer? %) (float? %)) [standard_dev x mean])
          (NumberEval. (normal-distribution x mean standard_dev cumulative))
          (StringEval. (str [x mean standard_dev])))))))

(def normdist-udf (AggregatingUDFFinder.  (into-array [(DefaultUDFFinder. (into-array ["_xlfn.NORM.DIST"])  (into-array [normdist-free]))])))

;;; NEGBINOM.DIST function implementation

(def pascal-distribution-free
  (reify FreeRefFunction
    (evaluate [this args ec]
      (let [row  (.getRowIndex ec)
            col  (.getColumnIndex ec)
            number_f (extract-operand args 0 row col)
            number_s (extract-operand args 1 row col)
            probability_s (extract-operand args 2 row col)
            cumulative (extract-operand args 3 row col)
            ]
		(if (every? #(or (integer? %) (float? %)) [number_f number_s probability_s])
          (NumberEval. (pascal-distribution number_f number_s probability_s cumulative))
          (StringEval. (str [number_f number_s probability_s cumulative])))))))

(def pascal-udf (AggregatingUDFFinder.  (into-array [(DefaultUDFFinder. (into-array ["_xlfn.NEGBINOM.DIST"])  (into-array [pascal-distribution-free]))])))

;;; BINOMIAL Distribution

(def binomial-distribution-free
  (reify FreeRefFunction
    (evaluate [this args ec]
      (let [row  (.getRowIndex ec)
            col  (.getColumnIndex ec)
            number_s (extract-operand args 0 row col)
            trials (extract-operand args 1 row col)
            probability_s (extract-operand args 2 row col)
            cumulative (extract-operand args 3 row col)
            ]
		(if (every? #(or (integer? %) (float? %)) [number_s probability_s trials])
          (NumberEval. (binomial-distribution number_s trials probability_s cumulative))
          (StringEval. (str [number_s trials probability_s cumulative])))))))

(def binomial-udf (AggregatingUDFFinder.
                   (into-array [(DefaultUDFFinder.
                                  (into-array ["_xlfn.BINOM.DIST"])
                                  (into-array [binomial-distribution-free]))])))


;;; BINOM.INV Distribution


(def binom-inv-free
  (reify FreeRefFunction
    (evaluate [this args ec]
      (let [row  (.getRowIndex ec)
            col  (.getColumnIndex ec)
            trials (extract-operand args 0 row col)
            probability_s (extract-operand args 1 row col)
            alpha (extract-operand args 2 row col)
            ]
		(if (every? #(or (integer? %) (float? %)) [trials probability_s alpha])
          (NumberEval. (double (binom-inv trials probability_s alpha)))  ;;; DOUBLE!!!!
          (StringEval. (str [trials probability_s alpha])))))))

(def binom-inv-udf (AggregatingUDFFinder.
                    (into-array [(DefaultUDFFinder.
                                   (into-array ["_xlfn.BINOM.INV"])
                                   (into-array [binom-inv-free]))])))


;; CRITBINOM WRAPPEE. FORMULA == BINOM.INV

(def critbinom-free
  (proxy [Fixed3ArgFunction] []
    (evaluate [col-index  row-index trials probability_s alpha]
      (let [trials-value (extract trials)
            probability_s-value (extract  probability_s)
            alpha-value (extract alpha)
            ]
		(if (every? #(or (integer? %) (float? %)) [trials-value probability_s-value alpha-value])
          (NumberEval. (double (binom-inv trials-value probability_s-value alpha-value)))
		  (StringEval. (str [trials-value probability_s-value alpha-value ]))
		  )))))

(register-fun! "CRITBINOM" critbinom-free)
