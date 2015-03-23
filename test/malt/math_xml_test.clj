(ns malt.math_xml_test
  (:use clojure.test
        [malt.math_parser.math_xls :as math]
        [malt.utils :as utils]
        ))

(comment
(deftest cdf-test
  (testing "NORMAL DIST DOSEN'T WORKS"
    (is (utils/trim-equal 0.36944134 (math/normal-distribution 10 20 30 true)))))

(deftest pdf-test
  (testing "NORMAL DIST DOSEN'T WORKS"
    (is (utils/trim-equal 0.012579441 (math/normal-distribution 10 20 30 false)))))

(deftest pascal-cdf-test
  (testing "Pascal CDF DIST DOSEN'T WORKS"
    (is (utils/trim-equal 0.974383745 (math/pascal-distribution 10 20 0.8 true)))))

(deftest pascal-pdf-test
  (testing "Pascal PDF DIST DOSEN'T WORKS"
    (is (utils/trim-equal 0.023647262 (math/pascal-distribution 10 20 0.8 false)))))



(deftest binom-inv-test
  (testing "BINOM.INV/ CRITBINOM"
    (time (is (= 75 (math/binom-inv 100 0.75 0.5))))
    (time (is (= 75 (math/binom-inv 100 0.75 0.5))))))

)
