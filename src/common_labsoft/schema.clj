(ns common-labsoft.schema
  (:require [schema.coerce :as coerce]
            [schema-tools.core :as schema-tools]
            [schema.core :as s]
            [common-labsoft.time :as time]))

(def time-matchers {time/LocalDateTime time/coerce-to-local-date-time
                    time/LocalDate     time/coerce-to-local-date})

(def internalize-matchers (coerce/first-matcher [time-matchers
                                                 coerce/json-coercion-matcher]))

(defn coerce [value schema]
  (schema-tools/select-schema value schema internalize-matchers))

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