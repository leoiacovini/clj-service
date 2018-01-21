(ns common-labsoft.datomic.schema
  (:require [schema.core :as s]
            [common-labsoft.time :as time]
            [common-labsoft.misc :as misc])
  (:import [schema.core EnumSchema]
           [clojure.lang Associative]))

(defn schema->datomic-value [schema]
  (cond
    (= schema s/Str) :db.type/string
    (= schema s/Int) :db.type/long
    (instance? EnumSchema schema) :db.type/ref
    (= schema s/Keyword) :db.type/keyword
    (= schema s/Bool) :db.type/boolean
    (= schema s/Uuid) :db.type/uuid
    (instance? schema Associative) :db.type/ref
    (= schema time/LocalDate) :db.type/instant
    (= schema time/LocalDateTime) :db.type/instant))

(defn schema->meta-schema [schema]
  (cond
    (= schema s/Str) :meta.type/string
    (= schema s/Int) :meta.type/long
    (instance? EnumSchema schema) :meta.type/ref
    (= schema s/Keyword) :meta.type/keyword
    (= schema s/Bool) :meta.type/boolean
    (= schema s/Uuid) :meta.type/uuid
    (instance? schema Associative) :meta.type/ref
    (= schema time/LocalDateTime) :meta.type/local-date-time
    (= schema time/LocalDate) :meta.type/local-date))

(def meta-schema [{:db/ident       :meta/type
                   :db/valueType   :db.type/ref
                   :db/cardinality :db.cardinality/one
                   :db/doc         "Type Used for conversion"}])

(def meta-enums [{:db/ident :meta.type/string}
                 {:db/ident :meta.type/long}
                 {:db/ident :meta.type/ref}
                 {:db/ident :meta.type/keyword}
                 {:db/ident :meta.type/boolean}
                 {:db/ident :meta.type/uuid}
                 {:db/ident :meta.type/ref}
                 {:db/ident :meta.type/local-date-time}
                 {:db/ident :meta.type/local-date}])

(defn- schema->datomic-cardinality [schema] (if (seq? schema) :db.cardinality/many :db.cardinality/one))

(defn- uniqueness [{:keys [id unique]}]
  (cond
    (true? id) :db.unique/identity
    (true? unique) :db.unique/value
    :else nil))

(defn- field->attribute [name {:keys [schema index doc] :as settings}]
  (-> {:db/ident       name
       :db/valueType   (schema->datomic-value schema)
       :db/cardinality (schema->datomic-cardinality schema)
       :meta/type      (schema->meta-schema schema)}
      (misc/assoc-if :db/unique (uniqueness settings))
      (misc/assoc-if :db/doc doc)
      (misc/assoc-if :db/index index)))

(defn create-schema [skeleton]
  (mapv (fn [[k v]] (field->attribute k v)) skeleton))

(defn create-enums [enum]
  (mapv (fn [v] {:db/ident v}) enum))