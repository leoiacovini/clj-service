(ns common-labsoft.schema
  (:require [schema.coerce :as coerce]
            [schema.core :as s]
            [common-labsoft.time :as time]
            [common-labsoft.misc]
            [schema-tools.core :as schema-tools]
            [common-labsoft.exception :as exception])
  (:import [schema.core EnumSchema]
           [clojure.lang ExceptionInfo]))

(defn maybe [f & args] (try (apply f args) (catch Exception _ nil)))

(defn str->long [v]
  (Long/parseLong v))

(defn coerce-long [v]
  (or (maybe long v) (maybe str->long v) v))

(defn coerce-str [v]
  (if (keyword? v)
    (str (namespace v) "/" (name v))
    (str v)))

(defn coerce-bigdec [v]
  (bigdec v))

(defn coerce-bigint [v]
  (bigint v))

(def time-matchers {time/LocalDate     time/coerce-to-local-date
                    time/LocalDateTime time/coerce-to-local-date-time})
(def custom-matchers {s/Str      coerce-str
                      s/Int      coerce-long
                      BigDecimal coerce-bigdec
                      BigInteger coerce-bigint})

(def internalize-matchers (coerce/first-matcher [time-matchers
                                                 custom-matchers
                                                 coerce/json-coercion-matcher]))

(defn coerce [value schema]
  (try
    (schema-tools/select-schema value schema internalize-matchers)
    (catch ExceptionInfo e
      (exception/bad-request! {:cause e :value value :schema schema :type :could-not-coerce}))))

(defn coerce-if [value schema]
  (if (and schema value)
    (coerce value schema)
    value))

(declare skel->schema)
(defn render-schema [meta]
  (let [schema (or (:schema meta) meta)]
    (cond
      (instance? EnumSchema schema) schema
      (and (not (record? schema)) (map? schema)) (skel->schema schema)
      (seq? schema) (map render-schema schema)
      :else schema)))

(defn attribute->schema [[k meta]]
  (if (:required meta)
    [(s/required-key k) (render-schema meta)]
    [(s/optional-key k) (render-schema meta)]))

(defn skel->schema [skeleton]
  (into {} (map attribute->schema skeleton)))
