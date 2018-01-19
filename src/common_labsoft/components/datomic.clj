(ns common-labsoft.components.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [io.pedestal.log :as log]
            [common-labsoft.datomic.schema :as datomic.schema]
            [common-labsoft.protocols.config :as protocols.config]
            [common-labsoft.protocols.datomic :as protocols.datomic]))

(defn install-meta-schema! [conn]
  @(d/transact conn (concat datomic.schema/meta-schema
                            datomic.schema/meta-enums)))

(defn ensure-schemas! [conn {:keys [schemas enums]}]
  (install-meta-schema! conn)
  (doseq [en enums]
    @(d/transact conn (datomic.schema/create-enums en))
    (log/info :datomic :enum-installed :enum en))
  (doseq [schema schemas]
    @(d/transact conn (datomic.schema/create-schema schema))
    (log/info :datomic :schema-installed :schema schema)))

(defn create-connection! [endpoint settings]
  (try
    (d/create-database endpoint)
    (let [connection (d/connect endpoint)]
      (ensure-schemas! connection settings)
      connection)
    (catch Exception e
      (log/error :component :datomic
                 :error :create-connection-failed
                 :exception e))))

(defrecord Datomic [config settings conn]
  component/Lifecycle
  (start [this]
    (if conn
      this
      (let [endpoint (protocols.config/get! config :datomic-endpoint)]
        (println "Creating Datomic database and connection...")
        (assoc this :endpoint endpoint
                    :conn (create-connection! endpoint settings)))))

  (stop [this]
    (if-not conn
      this
      (do (d/release conn)
          (dissoc this :conn))))

  protocols.datomic/Datomic
  (connection [this]
    (:conn this))

  (db [this]
    (d/db (:conn this))))

(defn new-datomic [settings] (map->Datomic {:settings settings}))