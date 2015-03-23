(ns malt.calc_async_test
  (:use clojure.test
        clojure.tools.trace)
  (:require [malt.multi.calc_async :refer (chunking-jobs)]
            [malt.utils :refer (join-tail)]))

(deftest join-tail-test
  (testing "join tail test"
    (is (= '((2 2 3) (1 1)) (join-tail 1 '((1 1) (2 2) (3)))))))

(deftest chunking-jobs-test
  (testing "fix chunking jobs fun"
    (is (= 600 (count (chunking-jobs 1 (range 600)))))
    (is (= 2 (count (chunking-jobs 2 (range 1233)))))
    (is (= 10 (count (chunking-jobs 10 (range 611)))))))

