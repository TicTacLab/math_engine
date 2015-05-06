 (ns bench
   (:require [malt.math-parser.core :as c]
             [criterium.core :as cr]
             [org.httpkit.client :as http]
             [cheshire.core :as json]))

 (defn do-bench []
   (let [in-params-json (->> (slurp "in_params.clj")
                             (read-string)
                             (map #(json/generate-string
                                    {:id     33
                                     :ssid   "09bf989f-5b24-47bc-871e-1e824d4f4c62"
                                     :params %})))]
     (cr/bench
       @(http/post "http://localhost:3000/model/calc/binary"
                   {:body (rand-nth in-params-json)}))))
 