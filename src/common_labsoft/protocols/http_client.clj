(ns common-labsoft.protocols.http-client)

(defprotocol HttpClient
  (authd-req! [this data] [this data transform]  "Validates its token, and re-generate if needed")
  (raw-req! [this data] "Simple HTTP Request"))

(def IHttpClient (:on-interface HttpClient))
