(ns malt.response
  (:use [flatland.protobuf.core :only (protodef protobuf protobuf-dump protobuf-load)]
        [clojure.tools.logging :as log])
  (:import [outcome Outcome$OutcomeProto
                    Outcome$Packet
                    Outcome$ErrorProto]
           [flatland.protobuf PersistentProtocolBufferMap$Def]))

(def OutcomeProto (protodef Outcome$OutcomeProto))
(def Packet (protodef Outcome$Packet))
(def ErrorProto (protodef Outcome$ErrorProto))

(defprotocol OutcomeProtocol
  (pack [this] "pack to protobuf"))

(extend-protocol OutcomeProtocol
  nil
  (pack [this] this)
  java.lang.Long
  (pack [this] this)
  java.lang.Float
  (pack [this] this)
  java.lang.String
  (pack [this] this)
  java.lang.Integer
  (pack [this] this)
  )

(defrecord ErrorItem
  [field value error]
  OutcomeProtocol
  (pack [this] (protobuf ErrorProto
                         :field field
                         :value (str value)
                         :err error)))
(defn validate
  [combinator translator default-value field value]
  (if (combinator value)
    (->ErrorItem field value nil)
    (try (->ErrorItem field (translator value) nil)
         ;; blank field fill with default type value
         (catch Exception e (->ErrorItem field default-value nil)))))

;; validate funcs

(def is-float? #(instance? java.lang.Float %))
(def item-integer (partial validate integer? int 0))
(def item-string (partial validate string? str ""))
(def item-float (partial validate is-float? float (float 0.0)))

;; outcome item

(defrecord OutcomeItem
  [id market outcome coef param m_code o_code param2 mgp_code mn_code mgp_weight mn_weight timer error]
  OutcomeProtocol
  (pack [this]
    (let [errors (filter #(string? (:error %))
                         [id market outcome coef param m_code o_code param2 timer])]
      (protobuf OutcomeProto
                :id (:value id)
                :market (:value market)
                :outcome (:value outcome)
                :coef (:value coef)
                :param (:value param)
                :m_code (:value m_code)
                :o_code (:value o_code)
                :param2 (:value param2)
                :mgp_code (:value mgp_code)
                :mn_code (:value mn_code)
                :mgp_weight (:value mgp_weight)
                :mn_weight (:value mn_weight)
                :timer (:value timer)))))

(defn outcome-init [o]
  (-> o
      (update-in [:id] #(item-integer "id" %))
      (update-in [:market] #(item-string "market" %))
      (update-in [:outcome] #(item-string "outcome" %))
      (update-in [:coef] #(item-float "coef" %))
      (update-in [:param] #(item-float "param" %))
      (update-in [:m_code] #(item-string "m_code" %))
      (update-in [:o_code] #(item-string "o_code" %))
      (update-in [:param2] #(item-float "param2" %))
      (update-in [:mgp_code] #(item-string "mgp_code" %))
      (update-in [:mn_code] #(item-string "mn_code" %))
      (update-in [:mgp_weight] #(item-integer "mgp_weight" %))
      (update-in [:mn_weight] #(item-integer "mn_weight" %))
      (update-in [:timer] #(when % (item-integer "timer" %)))
      (assoc :error nil)
      (map->OutcomeItem)))

;; packet outcome
(defrecord PacketItem
  [type data]
  OutcomeProtocol
  (pack [this] (protobuf Packet :type type :data (map pack data))))

(defmulti packet-init :type)

(defmethod packet-init :OUTCOMES [outcomes]
  (->> outcomes
       (:data)
       (map outcome-init)
       (map pack)
       (assoc outcomes :data)
       (protobuf Packet)
       (protobuf-dump)))

(defmethod packet-init :ERROR [error]
  (->> error
       (protobuf Packet)
       (protobuf-dump)))