(ns malt.web.middleware)

(defmacro defmiddleware [nm params handler-params & body]
  `(defn ~nm ~params
     (fn ~handler-params
       ~@body)))


(defmiddleware wrap-with-web
  [h web] [req]
  (h (assoc req :web web)))
