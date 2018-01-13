(ns common-labsoft.datomic.schema
  (:require [schema.core :as s]
            [common-labsoft.misc :as misc])
  (:import [schema.core EnumSchema]
           [clojure.lang Associative]))

(def schema->datomic-value {s/Str       :db.type/string
                            s/Int       :db.type/long
                            EnumSchema  :db.type/ref
                            s/Keyword   :db.type/keyword
                            s/Bool      :db.type/boolean
                            s/Uuid      :db.type/uuid
                            Associative :db.type/ref})

(defn- schema->datomic-cardinality [schema] (if (seq? schema) :db.cardinality/many :db.cardinality/one))

(defn- uniqueness [{:keys [id unique]}]
  (cond
    (true? id) :db.unique/identity
    (true? unique) :db.unique/value
    :else nil))

(defn- field->attribute [name {:keys [schema index doc] :as settings}]
  (-> {:db/ident       name
       :db/valueType   (schema->datomic-value schema)
       :db/cardinality (schema->datomic-cardinality schema)}
      (misc/assoc-if :db/unique (uniqueness settings))
      (misc/assoc-if :db/doc doc)
      (misc/assoc-if :db/index index)))

(defn create-schema [skeleton]
  (mapv (fn [[k v]] (field->attribute k v)) skeleton))

(defn create-enums [enum]
  (mapv (fn [v] {:db/ident v}) enum))