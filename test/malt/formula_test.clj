(ns malt.formula-test
  (:use clojure.test
        clojure.tools.trace
        malt.math-parser.core
        [malt.math-parser.xls-read :as r]
        [malt.math-parser.math-xls :as m]))

(def path-test-model "math_models/for_test.xlsx")

(comment
(deftest formula-replace-test
  (is (= "POISSON(1,2,TRUE)" (m/poissson-replace-bool-int "POISSON(1,2,1)")))
  (is (= "NORMDIST(1,2,3,4)" (m/norm-dist-replace "_xlfn.NORM.DIST(1,2,3,4)"))))
)

(deftest formula-test
  (let [wb (r/read-workbook path-test-model)
        evaluator (do (r/udfs-wb wb)
                      (r/make-formula-evaluator wb))
        sheet  (->> (r/sheets wb) first)
        result (r/sheet-evaluator sheet evaluator)
        ]
    (is (= {:price 0.36944134018176367, :id 1.0} (first result)))
    (is (= {:price 0.023647261968885933, :id 2.0} (second result)))
    (is (= {:price 5.480047776096421E-7, :id 3.0} (nth result 2)))
    (is (= {:price "VALUE!", :id 4.0} (nth result 3)))
    (is (= {:price "formula error:42", :id 5.0} (nth result 4)))
    (is (= {:id 6.0} (nth result 5)))
    (is (= {:id  7.0} (nth result 6)))
    
    (comment
      (is (= {:price 0.36944134018176367  :id 1.0} (first result)))
      (is (= {:price 0.023647261968885933 :id 2.0} (second result)))
      (is (= {:price 0.75     :id 4.0}             (nth result 3)))
      (is (= {:price 0.5      :id 5.0}             (nth result 4)))
      (is (= {:price 1.0      :id 3.0} (nth result 2))))
                                        
    ))
