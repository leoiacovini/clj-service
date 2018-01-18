(ns common-labsoft.protocols.sqs)

(defprotocol SQS
  (produce! [this produce-map]))

(def ISQS (:on-interface SQS))
