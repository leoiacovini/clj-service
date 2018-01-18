(ns common-labsoft.system
  (:require [com.stuartsierra.component :as component]
            [common-labsoft.components.config :as components.config]
            [common-labsoft.components.s3-client :as components.s3-client]
            [common-labsoft.components.sqs :as components.sqs]
            [common-labsoft.components.pedestal :as components.pedestal]
            [common-labsoft.components.datomic :as components.datomic]
            [common-labsoft.components.webapp :as components.webapp]
            [common-labsoft.components.token :as components.token]
            [common-labsoft.components.crypto :as components.crypto]))

(def system (atom nil))

(defn system-map [{routes :routes
                   config-name :config-name
                   custom-system :custom-system
                   queues-settings :queues-settings}]
  (merge
    (component/system-map
      :config    (component/using (components.config/new-config config-name) [])
      :s3-auth   (component/using (components.s3-client/new-s3-client :s3-auth) [:config])
      :pedestal  (component/using (components.pedestal/new-pedestal routes) [:config :webapp])
      :datomic   (component/using (components.datomic/new-datomic {}) [:config])
      :token     (component/using (components.token/new-token) [:config :s3-auth])
      :crypto    (component/using (components.crypto/new-crypto) [:config])
      :sqs       (component/using (components.sqs/new-sqs queues-settings) [:config])
      :webapp    (component/using (components.webapp/new-webapp) [:config :datomic :token :crypto :sqs]))
    custom-system))

(defn bootstrap! [settings]
  (prn "Starting system!!")
  (let [new-system (-> (system-map settings)
                       component/start-system)]
    (prn "System started successfully!!")
    (reset! system new-system)))

(defn stop! []
  (component/stop-system @system)
  (reset! system nil))