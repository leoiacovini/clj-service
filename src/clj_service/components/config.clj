(ns clj-service.components.config
  (:require [com.stuartsierra.component :as component]
            [aero.core :as aero]
            [clj-service.protocols.config :as protocols.config]
            [schema.core :as s]))

(s/defrecord Config [config-name :- s/Str]
  component/Lifecycle
  (start [this]
    (assoc this :config (aero/read-config (clojure.java.io/resource config-name))))

  (stop [this]
    (dissoc this :config))

  protocols.config/Config
  (get! [this k]
    (if-let [value (get (:config this) k)]
      value
      (throw (ex-info "ConfigKeyNotFound" {:config (:config this) :key k}))))

  (get-maybe [this k]
    (get (:config this) k))

  (get-in! [this ks]
    (if-let [value (get-in (:config this) ks)]
      value
      (throw (ex-info "ConfigKeyNotFound" {:config (:config this) :keys ks}))))

  (get-in-maybe [this ks]
    (get-in (:config this) ks)))

(s/defn new-config :- protocols.config/IConfig
  [config-name :- s/Str]
  (->Config config-name))
