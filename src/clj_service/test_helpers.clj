(ns clj-service.test-helpers
  (:require [clj-service.exception :as exception]
            [clj-service.protocols.config :as protocols.config]
            [io.pedestal.test :refer [response-for]]
            [cheshire.core :as chesire]
            [io.pedestal.http :as http]
            [clj-service.pedestal.interceptors.auth]
            [clj-service.time :as time]
            [cheshire.core :as cheshire]))

(defonce ^:private service-fn (atom nil))

(defn mock-config [obj]
  (reify
    protocols.config/Config
    (get! [this k] (or (get obj k) (exception/not-found! {})))
    (get-maybe [this k] (get obj k))
    (get-in-maybe [this ks] (get-in obj ks))
    (get-in! [this ks] (or (get-in obj ks) (exception/not-found! {})))))

(defmacro as-of [time & body]
  `(with-redefs [time/now (fn [] ~time)
                 time/today (fn [] ~time)]
     (do
       ~@body)))

(defn test-service
  "Return a service-fn for use with Pedestal's `response-for` test helper."
  [restart-fn]
  (let [system (restart-fn)]
    (reset! service-fn (::http/service-fn (-> system :pedestal :service)))))

(defmacro with-service [[start-fn stop-fn] [system-binding service-binding] & body]
  `(try
     (let [~system-binding (~start-fn)
           ~service-binding (::http/service-fn (-> ~system-binding :pedestal :service))
           result# (do ~@body)]
       result#)
     (finally
       (~stop-fn))))

(defn get-pedestal-service [system]
  (::http/service-fn (-> system :pedestal :service)))

(defmacro with-world [[wname restart-fn] & body]
  `(let [system# (~restart-fn)
         ~wname (atom {:system  system#
                       :service (get-pedestal-service system#)})]
     (do ~@body)))

(defn assoc-to-world [world key value]
  (swap! world assoc key value))

(defn get-in-world [world ks]
  (get-in @world ks))

(defn get-service [world]
  (get-in-world world [:service]))

(defn get-system [world]
  (get-in-world world [:system]))

(defn request!
  ([world method path body]
   (-> (response-for (get-service world) method path :body (cheshire/generate-string body) :headers {"Content-Type" "application/json"})
       :body
       (chesire/parse-string true)))
  ([world method path]
   (-> (response-for (get-service world) method path :headers {"Content-Type" "application/json"})
       :body
       (chesire/parse-string true))))
