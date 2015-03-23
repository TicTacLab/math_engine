(ns malt.math_parser.protocols)

(defprotocol CastClass
  (extract [this] [this value])
  (pack [this] [this value])
  (toString [this])
  (toMap [this] ))

