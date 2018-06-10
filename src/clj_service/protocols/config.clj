(ns clj-service.protocols.config)

(defprotocol Config
  (get! [this k])
  (get-maybe [this k])
  (get-in! [this ks])
  (get-in-maybe [this ks]))

(def IConfig (:on-interface Config))
