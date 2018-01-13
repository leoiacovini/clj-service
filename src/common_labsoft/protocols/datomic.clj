(ns common-labsoft.protocols.datomic
  (:require [schema.core :as s]))

(defprotocol Datomic
  )

(s/defschema IDatomic (:on-interface Datomic))