(ns malt.calculator-test
  (:require [malt.system :as s]
            [malt.calculator :as parser]
            [malt.session :as sess]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [malt.config :as c]))

(deftest calc-test
  (let [sys (component/start (s/new-system @c/config))]
    (try
      (let [session-store (:session-store sys)
            workbook-config (sess/config-to-workbook {:file           (io/file "test/malt/test-model.xls")
                                                      :in_sheet_name  "IN"
                                                      :out_sheet_name "OUT"
                                                      :id             1})]
        (sess/prolong! session-store "BADA55" workbook-config)
        (let [result (->> (parser/calc* workbook-config
                                        {:model_id 1 :event_id "BADA55" :params [{:id 1 :value 1.0}
                                                                                 {:id 2 :value 1.0}
                                                                                 {:id 3 :value 1.0}
                                                                                 {:id 4 :value 1.0}]})
                          set)]
          (is (= #{{:coef    2.0
                    :id      1.0
                    :m_code  "MATCH_BETTING"
                    :market  "3 way - Who will win the match"
                    :o_code  "HOME"
                    :outcome 1.0
                    :param   999999.0
                    :mn_weight 1.0}
                   {:coef    2.0
                    :id      2.0
                    :m_code  "MATCH_BETTING"
                    :market  "3 way - Who will win the match"
                    :o_code  "DRAW"
                    :outcome "X"
                    :param   999999.0
                    :mn_weight 2.0}
                   {:coef    2.0
                    :id      3.0
                    :m_code  "MATCH_BETTING"
                    :market  "3 way - Who will win the match"
                    :o_code  "AWAY"
                    :outcome 2.0
                    :param   999999.0
                    :mn_weight 3.0}
                   {:coef    2.0
                    :id      4.0
                    :m_code  "MATCH_DOUBLE_CHANCE"
                    :market  "Match Double Chance"
                    :o_code  "HOME_DRAW"
                    :outcome "1X"
                    :param   999999.0
                    :mn_weight 4.0}}
                 result))))
      (finally
        (component/stop sys)))))
