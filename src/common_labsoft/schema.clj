(ns common-labsoft.schema
  (:require [schema.coerce :as coerce]
            [schema-tools.core :as schema-tools]
            [schema.core :as s]))

(defn coerce [value schema]
  (schema-tools/select-schema value schema coerce/json-coercion-matcher))

(defn coerce-if [value schema]
  (if (and schema value)
    (coerce value schema)
    value))

(declare skel->schema)
(defn render-schema [meta]
  (let [schema (or (:schema meta) meta)]
    (cond
      (map? schema) (skel->schema schema)
      (seq? schema) (map render-schema schema)
      :else schema)))

(defn attribute->schema [[k meta]]
  (if (:required meta)
    [(s/required-key k) (render-schema meta)]
    [(s/optional-key k) (render-schema meta)]))

(defn skel->schema [skeleton]
  (into {} (map attribute->schema skeleton)))