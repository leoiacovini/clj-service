(ns common-labsoft.datomic.transform
  (:require [common-labsoft.time :as time]
            [datomic.api :as d]))

(defn schema-definition [attr db]
  (ffirst (d/q '[:find ?type
                 :in $ ?attr
                 :where [?e :db/ident ?attr]
                        [?e :meta/type ?teid]
                        [?teid :db/ident ?type]] db attr)))

(defn clj->datomic [value]
  (cond
    (instance? time/LocalDateTime value) (time/local-date-time->inst value)
    (instance? time/LocalDate value) (time/local-date->inst value)
    :else value))

(defn datomic->clj [attr value db]
  (let [meta-type (schema-definition attr db)]
    (case meta-type
      :meta.type/local-date-time (time/inst->local-date-time value)
      :meta.type/local-date (time/inst->local-date value)
      value)))

(defn transform-to-datomic [data]
  (clojure.walk/prewalk clj->datomic data))

(defn transform-from-datomic [data db]
  (let [f (fn [[k v]] [k (datomic->clj k v db)])]
    (clojure.walk/postwalk (fn [x]
                             (if (map? x) (into {} (map f x)) x)) data)))
