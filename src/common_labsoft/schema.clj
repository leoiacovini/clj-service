(ns common-labsoft.schema
  (:require [schema.coerce :as coerce]
            [schema-tools.core :as schema-tools]
            [schema.core :as s]))

(defn coerce [value schema]
  (schema-tools/select-schema value schema coerce/json-coercion-matcher))

(declare skel->schema)
(defn render-schema [schema]
  (if (map? schema)
    (skel->schema schema)
    schema))

(defn attribute->schema [[k meta]]
  (if (:required meta)
    [(s/required-key k) (render-schema (:schema meta))]
    [(s/optional-key k) (render-schema (:schema meta))]))

(defn skel->schema [skeleton]
  (into {} (map attribute->schema skeleton)))