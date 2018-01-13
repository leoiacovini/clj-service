(ns common-labsoft.protocols.crypto)

(defprotocol Crypto
  (bcrypt [this value])
  (check  [this encrypted attempt]))

(def ICrypto (:on-interface Crypto))