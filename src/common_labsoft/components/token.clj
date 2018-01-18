(ns common-labsoft.components.token
  (:require [common-labsoft.protocols.config :as protocols.config]
            [common-labsoft.protocols.token :as protocols.token]
            [buddy.sign.jwt :as jwt]
            [buddy.core.keys :as buddy.keys]
            [clj-time.core :as time]
            [com.stuartsierra.component :as component]
            [common-labsoft.protocols.s3-client :as protocols.s3-client]
            [io.pedestal.log :as log]))

(defn expiration-time [duration]
  (time/plus (time/now) (time/minutes duration)))

(defn try-pri-key [token pri-path]
  (try
    (assoc token :pri-key (-> (protocols.s3-client/get-object (:s3-auth token) pri-path)
                              buddy.keys/str->private-key))
    (catch Throwable e
      (log/warn :error :error-fetching-private-key :path pri-path :exception e)
      token)))

(defrecord Token [config s3-auth]
  component/Lifecycle
  (start [this]
    (-> (assoc this :pub-key (-> (protocols.s3-client/get-object s3-auth "pub-key.pem")
                                 buddy.keys/str->public-key))
        (try-pri-key "pri-key.pem")))

  (stop [this]
    (dissoc this :pub-key :pri-key))

  protocols.token/Token
  (encode [this content]
    (-> content
        (assoc :exp (expiration-time (protocols.config/get! config :jwt-duration))
               :iss "tudo-prontaum"
               :aud "user")
        (jwt/sign (:pri-key this) {:alg :es256})))

  (decode [this token]
    (try
      (jwt/unsign token (:pub-key this) {:alg :es256})
      (catch Exception _ nil)))
  (verify [this token]
    (protocols.token/decode this token)))

(defn new-token []
  (map->Token {}))