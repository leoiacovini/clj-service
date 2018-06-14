(ns clj-service.components.crypto
  (:require [com.stuartsierra.component :as component]
            [clj-service.protocols.crypto :as protocols.crypto]
            [clj-service.protocols.config :as protocols.config]
            [buddy.hashers :as hashers]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :as codecs]
            [buddy.core.bytes :as bytes]
            [buddy.core.crypto :as crypto]
            [buddy.sign.jwt :as jwt]))

(defrecord Crypto [config]
  component/Lifecycle
  (start [this]
    (let [crypto-config (protocols.config/get! config :crypto)]
      (assoc this :secret (:secret crypto-config)
                  :bcrypt-iterations (or (:bcrypt-iterations crypto-config) 10)
                  :key (hash/sha256 (:secret crypto-config))
                  :iv (bytes/slice (hash/sha256 (:iv crypto-config)) 0 16))))

  (stop [this]
    (dissoc this :secret :bcrypt-iterations :key :iv))

  protocols.crypto/Crypto
  (bcrypt [this value]
    (hashers/derive value {:alg :bcrypt+sha512 :iterations (:bcrypt-iterations this)}))

  (bcrypt-check [this value hash]
    (hashers/check value hash {:alg :bcrypt+sha512 :iterations (:bcrypt-iterations this)}))

  (sha256 [this value]
    (codecs/bytes->hex (hash/sha256 value)))

  (encrypt [this value]
    (crypto/encrypt (codecs/to-bytes value) (:key this) (:iv this) {:alg :aes128-cbc-hmac-sha256}))

  (decrypt [this value]
    (-> (crypto/decrypt value (:key this) (:iv this) {:alg :aes128-cbc-hmac-sha256})
        (codecs/bytes->str)))

  (jwt-sign [this value]
    (jwt/sign value (:secret this) {:alg :hs256}))

  (jwt-decode [this value validate]
    (jwt/unsign value (:secret this) {:alg :hs256 :skip-validation (not validate)}))

  (jwt-decode [this value]
    (protocols.crypto/jwt-decode this value true))

  (jwt-encrypt [this value]
    (jwt/encrypt value (:key this) {:alg :dir :enc :a128cbc-hs256}))

  (jwt-decrypt [this value validate]
    (jwt/decrypt value (:key this) {:alg :dir :enc :a128cbc-hs256 :skip-validation (not validate)}))

  (jwt-decrypt [this value]
    (protocols.crypto/jwt-decrypt this value true)))
