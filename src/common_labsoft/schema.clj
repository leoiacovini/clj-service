(ns common-labsoft.schema
  (:require [schema.coerce :as coerce]
            [schema.core :as s]
            [common-labsoft.time :as time]
            [schema.utils :as su]
            [schema.spec.core :as ss]))

(defn maybe [f & args] (try (apply f args) (catch Exception _ nil)))

(defn str->long [v]
  (Long/parseLong v))

(defn coerce-long [v]
  (or (maybe long v) (maybe str->long v) v))

(defn coerce-str [v]
  (if (keyword? v)
    (str (namespace v) "/" (name v))
    (str v)))

(def time-matchers {time/LocalDate     time/coerce-to-local-date
                    time/LocalDateTime time/coerce-to-local-date-time})
(def custom-matchers {s/Str coerce-str
                      s/Int coerce-long})

(def internalize-matchers (coerce/first-matcher [time-matchers
                                                 custom-matchers
                                                 coerce/json-coercion-matcher]))

(defn- coerce-or-error! [value schema coercer type]
  (let [coerced (coercer value)]
    (if-let [error (su/error-val coerced)]
      (throw
        (ex-info
          (str "Could not coerce value to schema: " (pr-str error))
          {:type type :schema schema :value value :error error}))
      coerced)))

(defn coercer
  "Produce a function that simultaneously coerces and validates a value against a `schema.`
  If a value can't be coerced to match the schema, an `ex-info` is thrown - like `schema.core/validate`,
  but with overridable `:type`, defaulting to `:schema-tools.coerce/error.`"
  ([schema]
   (coercer schema (constantly nil)))
  ([schema matcher]
   (coercer schema matcher ::error))
  ([schema matcher type]
   (let [coercer (coerce/coercer schema matcher)]
     (fn [value]
       (coerce-or-error! value schema coercer type)))))

(defn or-matcher
  "Creates a matcher where the first matcher matching the
  given schema is used."
  [& matchers]
  (fn [schema]
    (some #(% schema) matchers)))

(defn coerce-t
  "Simultaneously coerces and validates a value to match the given `schema.` If a `value` can't
  be coerced to match the `schema`, an `ex-info` is thrown - like `schema.core/validate`,
  but with overridable `:type`, defaulting to `:schema-tools.coerce/error.`"
  ([value schema]
   (coerce-t value schema (constantly nil)))
  ([value schema matcher]
   (coerce-t value schema matcher ::error))
  ([value schema matcher type]
   ((coercer schema matcher type) value)))

(defn- filter-schema-keys [m schema-keys extra-keys-checker]
  (reduce-kv
    (fn [m k _]
      (if (or (contains? schema-keys k)
              (and extra-keys-checker
                   (not (su/error? (extra-keys-checker k)))))
        m
        (dissoc m k)))
    m
    m))

(defn map-filter-matcher
  "Creates a matcher which removes all illegal keys from non-record maps."
  [schema]
  (when (and (map? schema) (not (record? schema)))
    (let [extra-keys-schema (s/find-extra-keys-schema schema)
          extra-keys-checker (when extra-keys-schema
                               (ss/run-checker
                                 (fn [s params]
                                   (ss/checker (s/spec s) params))
                                 true
                                 extra-keys-schema))
          explicit-keys (some->> (dissoc schema extra-keys-schema)
                                 keys
                                 (mapv s/explicit-schema-key)
                                 set)]
      (when (or extra-keys-checker (seq explicit-keys))
        (fn [x]
          (if (map? x)
            (filter-schema-keys x explicit-keys extra-keys-checker)
            x))))))

(defn select-schema
  "Strips all disallowed keys from nested Map schemas via coercion. Takes an optional
  coercion matcher for extra coercing the selected value(s) on a single sweep. If a value
  can't be coerced to match the schema `ExceptionInfo` is thrown (like `schema.core/validate`)."
  ([value schema]
   (select-schema value schema (constantly nil)))
  ([value schema matcher]
   (coerce-t value schema (or-matcher map-filter-matcher matcher))))

(defn coerce [value schema]
  (select-schema value schema internalize-matchers))

(defn coerce-if [value schema]
  (if (and schema value)
    (coerce value schema)
    value))

(declare skel->schema)
(defn render-schema [meta]
  (let [schema (or (:schema meta) meta)]
    (cond
      (and (not (record? schema)) (map? schema)) (skel->schema schema)
      (seq? schema) (map render-schema schema)
      :else schema)))

(defn attribute->schema [[k meta]]
  (if (:required meta)
    [(s/required-key k) (render-schema meta)]
    [(s/optional-key k) (render-schema meta)]))

(defn skel->schema [skeleton]
  (into {} (map attribute->schema skeleton)))
