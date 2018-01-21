(ns common-labsoft.test-helpers
  (:require [common-labsoft.exception :as exception]
            [common-labsoft.protocols.config :as protocols.config]
            [common-labsoft.datomic.api :as datomic]
            [io.pedestal.test :refer [response-for]]
            [cheshire.core :as chesire]
            [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [datomic.api :as d]
            [common-labsoft.datomic.transform :as datomic.transform]
            [common-labsoft.components.datomic :as components.datomic]
            [common-labsoft.time :as time]
            [common-labsoft.misc :as misc]
            [cheshire.core :as cheshire]))

(defonce ^:private service-fn (atom nil))

(defn mock-config [obj]
  (reify
    protocols.config/Config
    (get! [this k] (or (get obj k) (exception/not-found! {})))
    (get-maybe [this k] (get obj k))
    (get-in-maybe [this ks] (get-in obj ks))
    (get-in! [this ks] (or (get-in obj ks) (exception/not-found! {})))))

(defn db-with-entities
  "Return a db with seed tx-data applied"
  ([dtm entities]
   (-> (datomic.api/db dtm)
       (d/with (map datomic.transform/transform-to-datomic entities))
       :db-after)))

(defn request!
  ([service method path body]
   (-> (response-for service method path :body (cheshire/generate-string body) :headers {"Content-Type" "application/json"})
       :body
       (chesire/parse-string true)))
  ([service method path]
   (-> (response-for service method path :headers {"Content-Type" "application/json"})
       :body
       (chesire/parse-string true))))

(defmacro as-of [time & body]
  `(with-redefs [time/now (fn [] ~time)
                 time/today (fn [] ~time)]
     ~@body))

(defn clean-datomic [dtm]
  (d/delete-database (:endpoint dtm))
  (component/stop dtm)
  (component/start dtm))

(defn transact-entities [datomic entities]
  @(d/transact (datomic/conn datomic) (map datomic.transform/transform-to-datomic entities))
  datomic)

(defn new-mock-datomic [datomic-settings]
  (let [config (mock-config {:datomic-endpoint (str "datomic:mem://" (misc/uuid))})]
    (component/start (components.datomic/map->Datomic {:settings datomic-settings
                                                       :config   config}))))

(defmacro with-entities [datomic-settings [datomic-binding db-binding] entities & body]
  `(do
     (let [~datomic-binding (new-mock-datomic ~datomic-settings)
           _# (transact-entities ~datomic-binding ~entities)
           ~db-binding (datomic/db ~datomic-binding)
           result# (do ~body)]
       (d/delete-database (:endpoint ~datomic-binding))
       (component/stop ~datomic-binding)
       (d/release (datomic/conn ~datomic-binding))
       result#)))

(defn test-service
  "Return a service-fn for use with Pedestal's `response-for` test helper."
  [restart-fn]
  (let [system (restart-fn)]
    (reset! service-fn (::http/service-fn (-> system :pedestal :service)))))

(defmacro with-service [[start-fn stop-fn] [system-binding service-binding] & body]
  `(let [~system-binding (~start-fn)
         ~service-binding (::http/service-fn (-> ~system-binding :pedestal :service))
         result# (do ~@body)]
     (~stop-fn)
     result#))
