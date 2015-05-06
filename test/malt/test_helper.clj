(ns malt.test-helper)

(defn roughly= [x y]
  (< (Math/abs (- x y))
     0.0001))
