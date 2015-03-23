(ns malt.sbo_test
  (:use clojure.test
        clojure.tools.trace
        malt.feeds.sbobet))

(comment
(def m [
        #malt.feeds.sbobet_markets.MarketTotal{:param 2.5, :over 3.04, :under 1.39}
        #malt.feeds.sbobet_markets.MarketAsianHandicap{:param 0.25, :p1 3.43, :p2 1.33}
        #malt.feeds.sbobet_markets.MarketAsianHandicap{:param 0.0, :p1 1.78, :p2 2.16}
        #malt.feeds.sbobet_markets.MarketTotal{:param 2.75, :over 4.44, :under 1.19}])


(deftest sbo-market-test
  (is (= [{:id 1, :value 0.0}
          {:id 2, :value 1.78}
          {:id 3, :value 2.16}
          {:id 4, :value 2.5 }
          {:id 5, :value 3.04}
          {:id 6, :value 1.39}]
         (vec (markets-to-malt m)))))


(deftest market-compare-test
  (is (=
       #malt.feeds.sbobet_markets.MarketTotal{:param 2.5, :over 3.04, :under 1.39}
       (.market-compare
        #malt.feeds.sbobet_markets.MarketTotal{:param 2.5, :over 3.04, :under 1.39}
        #malt.feeds.sbobet_markets.MarketTotal{:param 2.75, :over 4.44, :under 1.19}
        )))
  (is (=
       #malt.feeds.sbobet_markets.MarketAsianHandicap{:param 0.0, :p1 1.78, :p2 2.16}
       (.market-compare
        #malt.feeds.sbobet_markets.MarketAsianHandicap{:param 0.25, :p1 3.43, :p2 1.33}
        #malt.feeds.sbobet_markets.MarketAsianHandicap{:param 0.0, :p1 1.78, :p2 2.16}
        ))))
)
