(ns malt.param_field_test
  (:use clojure.test
        malt.views.param_field))
 
 
;(deftest test-dispatch
;  (is (=
;       'interval
;       (dispatch 1 0.0 "[interval 0.0 10.0 0.25]")))
;  (is (= nil (dispatch 1 0.0 nil))))

(comment
(deftest test-field
  (is (=  "<p>1<input id=\"1\" name=\"1\" type=\"text\" value=\"0.0\" /></p>" (-> (dispatch 1 0.0 nil) .field)))
  (is  (= '[:select {:name "1", :id "1"} ([:option {:selected false} 0.0] [:option {:selected false} 0.25])]
          (-> (dispatch 1 0.0 "[interval 0.0 0.5 0.25]") .field ))))
)