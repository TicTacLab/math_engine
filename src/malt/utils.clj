(ns malt.utils)

(defn string-to-double [x]
  (cond
    (float? x) x
    (string? x) (Double/parseDouble x)
    (number? x) (double x)))

(defn string-to-integer [x]
  (if (integer? x)
    x
    (Integer/parseInt x)))