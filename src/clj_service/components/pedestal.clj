(ns clj-service.components.pedestal
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.service-tools.dev :as dev]
            [io.pedestal.http :as server]
            [clj-service.protocols.config :as protocols.config]))

(defn base-service [routes port] {:env                     :prod
                                  ::http/routes            routes
                                  ::http/type              :jetty
                                  ::http/join?             true
                                  ::http/port              (or port 8080)
                                  ::http/container-options {:h2c? true
                                                            :h2?  false
                                                            :ssl? false}})

(defn inject-components [webapp]
  {:name  ::inject-components
   :enter (fn [context]
            (assoc-in context [:request :components] webapp))})

(defn add-system [service-map webapp]
  (update-in service-map [::http/interceptors] #(vec (cons (inject-components webapp) %))))

(defn run-dev
  [service-map routes-var webapp]
  (println "\nCreating your [DEV] server...")
  (let [dev-service (-> service-map
                        (merge {:env                     :dev
                                ::http/routes            (dev/watch-routes-fn routes-var)
                                ::server/join?           false
                                ::server/allowed-origins {:creds true :allowed-origins (constantly true)}
                                ::server/secure-headers  {:content-security-policy-settings {:object-src "none"}}})
                        server/default-interceptors
                        server/dev-interceptors
                        (add-system webapp)
                        server/create-server
                        server/start)]
    (dev/watch)
    dev-service))

(defn run-prod
  [service-map webapp]
  (println "\nCreating your [PROD] server...")
  (-> service-map
      server/default-interceptors
      (add-system webapp)
      http/create-server
      http/start))

;; TODO: Refactor this and make it more customizable and stable
(defrecord PedestalServer [routes-var config service webapp]
  component/Lifecycle
  (start [this]
    (if service
      this
      (let [service-config (base-service @routes-var (protocols.config/get-maybe config :port))]
        (if (= "dev" (protocols.config/get-maybe config :env))
          (assoc this :service (run-dev service-config routes-var webapp))
          (assoc this :service (run-prod service-config webapp))))))

  (stop [this]
    (when service
      (println "Stopping Pedestal Server...")
      (http/stop service))
    (dissoc this :service)))

(defn new-pedestal [routes-var]
  (map->PedestalServer {:routes-var routes-var}))
