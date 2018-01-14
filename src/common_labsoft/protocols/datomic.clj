(ns common-labsoft.protocols.datomic)

(defprotocol Datomic
  (connection [this])
  (db [this]))

(def IDatomic (:on-interface Datomic))