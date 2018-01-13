(ns common-labsoft.datomic.api
  (:require [datomic.api :as d]
            [common-labsoft.protocols.datomic :as protocols.datomic]
            [schema.core :as s]
            [common-labsoft.exception :as exception]))

(defn db [datomic] (protocols.datomic/db datomic)) )

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
    (ffirst result)))

(defn lookup [id-key id db]
  (query-single! '{:find  [(pull ?e [*])]
                   :in    [$ ?attr ?id]
                   :where [[?e ?attr ?id]]} db id-key id))

(defn lookup! [id-key id db]
  (or (lookup id-key id db) (exception/not-found! {:datomic :entity-not-found
                                                   :id-key  id-key
                                                   :id      id})))

(defn insert! [id-key entity datomic]
  (let [prepared-entity (update entity id-key #(or % (d/squuid)))
        {:keys [db-after]} @(d/transact (protocols.datomic/connection datomic) [prepared-entity])]
    (lookup! id-key (get prepared-entity id-key) db-after)))

(defn update! [id-key entity datomic]
  (assert-id! id-key entity)
  (let [prepared-entity (assoc entity :db/id [id-key (get entity id-key)])]
    @(d/transact (protocols.datomic/connection datomic) [prepared-entity])))

(defn retract! [id-key entity datomic]
  (assert-id! id-key entity)
  @(d/transact (protocols.datomic/connection datomic) [[:db.fn/retractEntity [id-key (get entity id-key)]]]))