(ns common-labsoft.schema
  (:require [schema.coerce :as coerce]
            [schema-tools.core :as schema-tools]))

(defn coerce [value schema]
  (schema-tools/select-schema value schema coerce/json-coercion-matcher))