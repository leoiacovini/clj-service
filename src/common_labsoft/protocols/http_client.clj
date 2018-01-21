(ns common-labsoft.protocols.http-client)

(defprotocol HttpClient
  (get-hosts [this] "Returns a map with all services names and endpoints")
  (authd-req! [this data] [this data transform]  "Validates its token, and re-generate if needed")
  (authd-get! [this data])
  (authd-post! [this data])
  (authd-delete! [this data])
  (authd-patch! [this data])
  (authd-put! [this data])
  (raw-req! [this data] "Simple HTTP Request"))

(def IHttpClient (:on-interface HttpClient))