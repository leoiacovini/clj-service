(ns common-labsoft.datomic.api
  (:require [datomic.api :as d]
            [common-labsoft.protocols.datomic :as protocols.datomic]
            [common-labsoft.datomic.transform :as datomic.transform]
            [schema.core :as s]
            [common-labsoft.exception :as exception]))

(defn db [datomic] (protocols.datomic/db datomic))
(defn conn [datomic] (protocols.datomic/connection datomic))

(defn extract-entity [entity]
  (clojure.walk/prewalk
    (fn [x] (if (instance? datomic.query.EntityMap x) (into {} x) x)) entity))

(defn export-entity
  [entity db]
  (-> (extract-entity entity)
      (datomic.transform/transform-from-datomic db)))

(defn assert-id! [id-key entity]
  (when-not (get entity id-key)
    (exception/server-error! {:assert-error :entity-is-missing-id
                              :id-key       id-key
                              :entity       entity})))

(defn query-single! [query db & args]
  (let [result (apply d/q query db args)]
    (when (< 1 (count result)) (exception/server-error! {:assert-error :more-than-one-result
                                                         :query        query
                                                         :args         args}))
    (when (< 1 (count (first result))) (exception/server-error! {:assert-error :more-than-one-result
                                                                 :query        query
                                                                 :args         args}))
    (some-> (d/entity db (ffirst result))
            (export-entity db))))

(defn lookup [id-key id db]
  (query-single! '{:find  [?e]
                   :in    [$ ?attr ?id]
                   :where [[?e ?attr ?id]]} db id-key id))

(defn lookup! [id-key id db]
  (or (lookup id-key id db) (exception/not-found! {:datomic :entity-not-found
                                                   :id-key  id-key
                                                   :id      id})))

(defn insert! [id-key entity datomic]
  (let [prepared-entity (-> (update entity id-key #(or % (d/squuid)))
                            datomic.transform/transform-to-datomic)
        {:keys [db-after]} @(d/transact (conn datomic) [prepared-entity])]
    (lookup! id-key (get prepared-entity id-key) db-after)))

(defn update! [id-key entity datomic]
  (assert-id! id-key entity)
  (let [prepared-entity (-> (assoc entity :db/id [id-key (get entity id-key)])
                            datomic.transform/transform-to-datomic)
        {:keys [db-after]} @(d/transact (conn datomic) [prepared-entity])]
    (lookup! id-key (get prepared-entity id-key) db-after)))

(defn retract! [id-key entity datomic]
  (assert-id! id-key entity)
  @(d/transact (conn datomic) [[:db.fn/retractEntity [id-key (get entity id-key)]]]))

(defn entities [query db & args]
  (let [result (apply d/q query db args)]
    (mapv (fn [[eid]] (export-entity (d/entity db eid) db)) result)))