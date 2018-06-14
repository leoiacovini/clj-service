(ns clj-service.protocols.crypto)

(defprotocol Crypto
  (bcrypt [this value])
  (bcrypt-check [this value hash])
  (sha256 [this value])
  (encrypt [this value])
  (decrypt [this value])
  (jwt-sign [this value])
  (jwt-decode [this value] [this value validate])
  (jwt-encrypt [this value])
  (jwt-decrypt [this value] [this value validate]))

(def ICrypto (:on-interface Crypto))
