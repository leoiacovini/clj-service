(ns common-labsoft.protocols.http-client)

(defprotocol HttpClient
  (get-hosts [this] "Returns a map with all services names and endpoints")
  (authd-req! [this data] "Validates its token, and re-generate if needed")
  (raw-req! [this data] "Simple HTTP Request"))

(def IHttpClient (:on-interface HttpClient))