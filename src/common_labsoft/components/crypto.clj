(ns common-labsoft.components.crypto
  (:require [common-labsoft.protocols.crypto :as protocols.crypto]
            [buddy.hashers :as hashers]))

(defrecord Crypto [config]
  protocols.crypto/Crypto
  (bcrypt [this value]
    (hashers/derive value {:alg :bcrypt+sha512}))
  (check [this encrypted attempt]
    (hashers/check attempt encrypted {:limit :bcrypt+sha512})))

(defn new-crypto []
  (map->Crypto {}))