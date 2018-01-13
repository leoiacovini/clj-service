(ns common-labsoft.protocols.datomic
  (:require [schema.core :as s]))

(defprotocol Datomic
  (connection [this])
  (db [this]))

(s/defschema IDatomic (:on-interface Datomic))