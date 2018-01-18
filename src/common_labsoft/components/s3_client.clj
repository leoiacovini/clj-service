(ns common-labsoft.components.s3-client
  (:require [common-labsoft.protocols.s3-client :as protocols.s3-client]
            [com.stuartsierra.component :as component]
            [amazonica.aws.s3 :as s3]
            [common-labsoft.protocols.config :as protocols.config]))

(defrecord S3Client [config bucket-config-key]
  component/Lifecycle
  (start [this]
    (assoc this :bucket-name (protocols.config/get-in! config [bucket-config-key :bucket-name])
                :region (protocols.config/get-in! config [bucket-config-key :region])))

  (stop [this]
    (dissoc this :bucket-name :region))

  protocols.s3-client/S3Client
  (get-object [this path]
    (with-open [input-stream (:input-stream (s3/get-object {:endpoint (:region this)}
                                                           {:bucket-name (:bucket-name this)
                                                            :key         path}))]
      (slurp input-stream)))

  (put-object [this path]                                   ;; TODO
    ))

(defn new-s3-client [bucket-config-key]
  (map->S3Client {:bucket-config-key bucket-config-key}))