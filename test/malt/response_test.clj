(ns malt.response-test
  (:require [clojure.test :refer :all]
            [flatland.protobuf.core :as pb]
            [malt.response :refer :all])
  (:import [malt.response ErrorItem]))

(def test-outcome-1 {:id 1.0
                     :market "market-name"
                     :outcome "outcome-name"
                     :coef 1.2
                     :param 0.5
                     :m_code "m-code"
                     :o_code "o-code"})
(def test-outcome-2 {:id 2.0
                     :market "market-name"
                     :outcome "outcome-name"
                     :coef 1.2
                     :param "ERROR!!"
                     :m_code "m-code"
                     :o_code "o-code"})

(deftest
  coef-error-test
  (testing
    (let [error (ErrorItem. "id" nil "some error")]
      (is (= "some error" (:err (pack error)))))))

(deftest
  validators-test
  (testing
    (is (is-float? (:value (item-float "param" 1.0))))))

(deftest
  gen-outcome-message
  (testing
    (is (contains? (pb/protobuf-load Packet
                                  (packet-init {:type :OUTCOMES :data [test-outcome-1 test-outcome-2]})) :data))
    (is (= "error" (:error (pb/protobuf-load Packet (packet-init {:type :ERROR :error "error"})))))))


