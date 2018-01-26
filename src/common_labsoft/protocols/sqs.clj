(ns common-labsoft.protocols.sqs)

(defprotocol Producer
  (produce! [this produce-map]))

(defprotocol Consumer
  (start-consumers! [this])
  (stop-consumers! [this]))

(def IProducer (:on-interface Producer))
(def IConsumer (:on-interface Consumer))
