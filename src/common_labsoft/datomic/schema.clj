(ns common-labsoft.datomic.schema
  (:require [schema.core :as s]
            [common-labsoft.time :as time]
            [common-labsoft.misc :as misc])
  (:import [schema.core EnumSchema]
           [clojure.lang Associative]))

(def schema->datomic-value {s/Str              :db.type/string
                            s/Int              :db.type/long
                            EnumSchema         :db.type/ref
                            s/Keyword          :db.type/keyword
                            s/Bool             :db.type/boolean
                            s/Uuid             :db.type/uuid
                            Associative        :db.type/ref
                            time/LocalDate     :db.type/instant
                            time/LocalDateTime :db.type/instant})

(def schema->meta-schema {s/Str              :meta.type/string
                          s/Int              :meta.type/long
                          EnumSchema         :meta.type/ref
                          s/Keyword          :meta.type/keyword
                          s/Bool             :meta.type/boolean
                          s/Uuid             :meta.type/uuid
                          Associative        :meta.type/ref
                          time/LocalDateTime :meta.type/local-date-time
                          time/LocalDate     :meta.type/local-date})

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