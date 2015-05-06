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

(defmacro with-timer [& body]
  `(let [start# (. System (nanoTime))
        ret# (do ~@body)
        time-call#
        (/
         (double 
          (- (. System (nanoTime)) start#))
         1000000.0)]
    (assoc ret# :timer (int time-call#))))

