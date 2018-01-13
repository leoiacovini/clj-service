(ns common-labsoft.components.token
  (:require [common-labsoft.protocols.config :as protocols.config]
            [common-labsoft.protocols.token :as protocols.token]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]))

(defn expiration-time [duration]
  (time/plus (time/now) (time/minutes duration)))

(defrecord Token [config]
  protocols.token/Token
  (encode [this content]
    (-> content
        (assoc :exp (expiration-time (protocols.config/get! config :jwt-duration))
               :iss "3design"
               :aud "user")
        (jwt/sign (protocols.config/get! config :jwt-key))))
  (decode [this token]
    (try
      (jwt/unsign token (protocols.config/get! config :jwt-key))
      (catch Exception _ nil)))
  (verify [this token]
    (protocols.token/decode this token)))

(defn new-token []
  (map->Token {}))