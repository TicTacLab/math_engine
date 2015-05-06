(ns malt.math-xml-test
  (:require [clojure.test :refer :all]
            [malt.math-parser.math-xls :as math]
            [malt.test-helper :as th]))

(deftest cdf-test
  (testing "NORMALDISTS"
    (is (th/roughly= 0.36944134 (math/normal-distribution 10 20 30 true)))))

(deftest pdf-test
  (testing "NORMALDIST"
    (is (th/roughly= 0.012579441 (math/normal-distribution 10 20 30 false)))))

(deftest pascal-cdf-test
  (testing "Pascal CDF DIST"
    (is (th/roughly= 0.974383745 (math/pascal-distribution 10 20 0.8 true)))))

(deftest pascal-pdf-test
  (testing "Pascal PDF DIST"
    (is (th/roughly= 0.023647262 (math/pascal-distribution 10 20 0.8 false)))))

(deftest binom-inv-test
  (testing "BINOM.INV/ CRITBINOM"
    (is (= 75 (math/binom-inv 100 0.75 0.5)))
    (is (= 75 (math/binom-inv 100 0.75 0.5)))))


