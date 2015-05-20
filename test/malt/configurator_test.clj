(ns malt.configurator-test
  (:require [clojure.test :refer :all]
            [malt.configurator :refer :all]
            [cheshire.core :as json]))

(deftest configurator-test
  (let [config (read-config)]
    (testing "update part of config"
      (let [update-part {:hello "world"}
            new-config (-> config
                           (:body)
                           (json/parse-string true)
                           (assoc :hello "world")
                           (json/generate-string))]
        (update-config {:body (json/generate-string update-part)})
        (is (= new-config (:body (read-config)))
            "should update part of config")))))
