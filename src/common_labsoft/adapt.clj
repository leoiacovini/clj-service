(ns common-labsoft.adapt
  (:require [common-labsoft.time]
            [schema.core :as s]
            [cheshire.core :as cheshire]
            [common-labsoft.schema :as schema]))

(defmulti externalize (fn [format & args] format))

(defmethod externalize :json
  ([format data & [schema]]
    (-> data
        (schema/coerce-if schema)
        cheshire/generate-string)))

(defmethod externalize :edn
  [format data & [schema]]
  (-> data
      (schema/coerce-if schema)
      pr-str))

(defmulti internalize (fn [format & args] format))

(defmethod internalize :json
  [format data & [schema]]
  (-> data
      (cheshire/parse-string true)
      (schema/coerce-if schema)))

(defmethod internalize :edn
  [format data & [schema]]
  (-> data
      read-string
      (schema/coerce-if schema)))

(def from-json (partial internalize :json))
(def from-edn (partial internalize :edn))
(def to-json (partial externalize :json))
(def to-edn (partial externalize :edn))
