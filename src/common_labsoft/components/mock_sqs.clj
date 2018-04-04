(ns common-labsoft.components.mock-sqs
  (:require [io.pedestal.log :as log]
            [common-labsoft.time]
            [common-labsoft.protocols.sqs :as protocols.sqs]
            [com.stuartsierra.component :as component]
            [common-labsoft.fault-tolerance :as fault-tolerance]
            [common-labsoft.schema :as schema]))


(defn handle-with-retries [message webapp handler]
  (fault-tolerance/with-retries 3
    (handler message webapp))
  message)

(defn assoc-to-consumed [message consumer qname schema]
  (swap! (:consumed-messages consumer) update qname #(conj (or % []) (schema/coerce message schema))))

(defn handle-message! [{webapp :webapp queues :queues :as this} qname message]
  (let [{handler-fn :handler schema :schema name :name :as queue} (get queues qname)]
    (try
      (some-> message
              (schema/coerce schema)
              (handle-with-retries webapp handler-fn)
              (assoc-to-consumed this name schema))
      (catch Exception e
        (log/error :exception e :error :receiving-message :queue queue :message message)))))

(defn is-producer? [queue]
  (#{:producer :both} (:direction queue)))

(defn get-queue [sqs produce-map]
  (-> sqs
      :queues
      (get (:queue produce-map))))

(defn queue-config->queue [[qname qconf]]
  [qname (assoc qconf :name qname
                      :url (name qname))])

(defn gen-queue-map [{:keys [queues-settings] :as this}]
  (assoc this :queues (into {} (map queue-config->queue queues-settings))))

(defn produce! [{produced-messages :produced-messages} queue {message :message}]
  (if (is-producer? queue)
    (swap! produced-messages update (:name queue) #(conj (or % []) (schema/coerce message (:schema queue))))
    (log/error :error :producing-to-non-producer-queue :queue queue)))

(defrecord Consumer [config queues-settings webapp]
  component/Lifecycle
  (start [this]
    (prn "Starting Mock SQS Consumer...")
    (-> this
        (gen-queue-map)
        (assoc :consumed-messages (atom {}))))

  (stop [this]
    (dissoc this :queues))

  protocols.sqs/Consumer
  (start-consumers! [this] this)
  (stop-consumers! [this] this))

(defrecord Producer [config queues-settings]
  component/Lifecycle
  (start [this]
    (-> this
        (gen-queue-map)
        (assoc :produced-messages (atom {}))))

  (stop [this]
    (dissoc this :queues :endpoint))

  protocols.sqs/Producer
  (produce! [this produce-map]
    (produce! this (get-queue this produce-map) produce-map)))

(defn consume! [consumer queue message]
  (handle-message! consumer queue message))

(defn new-mock-consumer [queues-settings]
  (map->Consumer {:queues-settings queues-settings}))

(defn new-mock-producer [queues-settings]
  (map->Producer {:queues-settings queues-settings}))
