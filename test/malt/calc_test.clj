(ns malt.calc-test
  (:require [malt.system :as s]
            [malt.math_parser.core :as parser]
            [malt.session :as sess]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [environ.core :as environ]
            [clojure.java.io :as io]))

(deftest calc-test
  (let [sys (component/start (s/new-system environ/env))]
    (try
      (let [session-store (:session-store sys)]
        (sess/save! session-store "BADA55" (sess/config-to-workbook {:file           (io/file "math_models/test-model.xls")
                                                                     :in_sheet_name  "IN"
                                                                     :out_sheet_name "OUT"
                                                                     :id             1}))
        (let [result (->> (parser/calc* session-store {:id 1 :ssid "BADA55" :params [{:id 1 :value 1.0}
                                                                                    {:id 2 :value 1.0}
                                                                                    {:id 3 :value 1.0}
                                                                                    {:id 4 :value 1.0}]})
                          :data
                          set)]
          (is (= result
                 #{{:coef    2.0
                    :id      1.0
                    :m_code  "MATCH_BETTING"
                    :market  nil
                    :o_code  "HOME"
                    :outcome 1.0
                    :param   999999.0
                    :param2  nil}
                   {:coef    2.0
                    :id      2.0
                    :m_code  "MATCH_BETTING"
                    :market  nil
                    :o_code  "DRAW"
                    :outcome "X"
                    :param   999999.0
                    :param2  nil}
                   {:coef    2.0
                    :id      3.0
                    :m_code  "MATCH_BETTING"
                    :market  nil
                    :o_code  "AWAY"
                    :outcome 2.0
                    :param   999999.0
                    :param2  nil}
                   {:coef    2.0
                    :id      4.0
                    :m_code  "MATCH_DOUBLE_CHANCE"
                    :market  "Match Double Chance"
                    :o_code  "HOME_DRAW"
                    :outcome "1X"
                    :param   999999.0
                    :param2  nil}}))))
      (finally
        (component/stop sys)))))
