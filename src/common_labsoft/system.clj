(ns common-labsoft.system
  (:require [com.stuartsierra.component :as component]
            [common-labsoft.components.config :as components.config]
            [common-labsoft.components.pedestal :as components.pedestal]
            [common-labsoft.components.datomic :as components.datomic]
            [common-labsoft.components.webapp :as components.webapp]))

(def system (atom nil))

(defn system-map [{routes :routes config-name :config-name}]
  (component/system-map
    :config   (component/using (components.config/new-config config-name) [])
    :pedestal (component/using (components.pedestal/new-pedestal routes) [:config :webapp])
    :datomic  (component/using (components.datomic/new-datomic {}) [:config])
    :webapp   (component/using (components.webapp/new-webapp) [:config :datomic])))

(defn bootstrap! [config]
  (prn "Starting system!!")
  (let [new-system (-> (system-map config)
                       component/start-system)]
    (reset! system new-system)))

(defn stop! []
  (component/stop-system @system)
  (reset! system nil))