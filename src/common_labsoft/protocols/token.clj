(ns common-labsoft.protocols.token)

(defprotocol Token
  (encode [this content])
  (decode [this token])
  (verify [this token]))

(def IToken (:on-interface Token))