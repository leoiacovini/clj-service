(ns common-labsoft.protocols.s3-client)

(defprotocol S3Client
  (get-object [this path])
  (put-object [this path]))

(def IS3Client (:on-interface S3Client))