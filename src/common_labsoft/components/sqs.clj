(ns common-labsoft.components.sqs
  (:require [clojure.core.async :as async]
            [amazonica.aws.sqs :as sqs]
            [io.pedestal.log :as log]
            [common-labsoft.misc :as misc]
            [common-labsoft.time]
            [common-labsoft.adapt :as adapt]
            [common-labsoft.protocols.sqs :as protocols.sqs]
            [com.stuartsierra.component :as component]
            [common-labsoft.protocols.config :as protocols.config]
            [common-labsoft.fault-tolerance :as fault-tolerance]))

(defn receive-messages! [endpoint queue]
  (some->> (sqs/receive-message {:endpoint endpoint} (:url queue))
           :messages
           (map #(assoc % :queue-url (:url queue)))))

(defn parse-message [message schema]
  (-> message :body (adapt/from-json schema)))

(defn fetch-message! [endpoint queue queue-channel]
  (async/go-loop []
    (doseq [message (receive-messages! endpoint queue)]
      (async/>! queue-channel message))
    (recur)))

(defn handle-with-retries [message webapp handler]
  (fault-tolerance/with-retries 3
    (handler message webapp)))

(defn handle-message! [endpoint webapp {handler-fn :handler schema :schema :as queue} queue-channel]
  (async/go-loop []
    (when-let [message (async/<! queue-channel)]
      (try
        (some-> message
                (parse-message schema)
                (handle-with-retries webapp handler-fn))
        (sqs/delete-message {:endpoint endpoint} message)
        (catch Throwable e
          (log/error :exception e :error :receving-message :queue queue :message message))))
    (recur)))

(defn is-consumer? [queue]
  (#{:consumer :both} (:direction queue)))

(defn is-producer? [queue]
  (#{:producer :both} (:direction queue)))

(defn get-queue [sqs produce-map]
  (-> sqs
      :queues
      (get (:queue produce-map))))

(defn consumer-started? [queue]
  (and (is-consumer? queue)
       (some? (:chan queue))))

(defn create-consumer-loop! [endpoint webapp queue]
  (let [queue-channel (async/chan 50)]
    (fetch-message! endpoint queue queue-channel)
    (handle-message! endpoint webapp queue queue-channel)
    (assoc queue :chan queue-channel)))

(defn init-async-consumer! [endpoint webapp queue]
  (if (is-consumer? queue)
    (create-consumer-loop! endpoint webapp queue)
    queue))

(defn stop-async-consumer! [queue]
  (when (consumer-started? queue)
    (async/close! (:chan queue)))
  (dissoc queue :chan))

(defn start-consumers! [{:keys [endpoint webapp] :as this}]
  (update this :queues #(misc/map-vals (partial init-async-consumer! endpoint webapp) %)))

(defn stop-consumers! [queues]
  (misc/map-vals stop-async-consumer! queues))

(defn find-or-create-queue! [endpoint qname]
  (or (sqs/find-queue {:endpoint endpoint} qname)
      (:queue-url (sqs/create-queue {:endpoint endpoint}
                                    :queue-name qname
                                    :attributes {:ReceiveMessageWaitTimeSeconds 20}))))

(defn queue-config->queue [endpoint [qname qconf]]
  [qname (assoc qconf :name qname
                      :url (find-or-create-queue! endpoint (name qname)))])

(defn gen-queue-map [{:keys [queues-settings endpoint] :as this}]
  (assoc this :queues (into {} (map (partial queue-config->queue endpoint) queues-settings))))

(defn produce! [{endpoint :endpoint} queue {message :message schema :schema}]
  (if (is-producer? queue)
    (sqs/send-message {:endpoint endpoint} (:url queue) (adapt/to-json message schema))
    (log/error :error :producing-to-non-producer-queue :queue queue)))

(defrecord Consumer [config queues-settings webapp]
  component/Lifecycle
  (start [this]
    (prn "Starting SQS Consumer...")
    (-> (assoc this :endpoint (protocols.config/get-in-maybe config [:sqs :endpoint]))
        (gen-queue-map)
        protocols.sqs/start-consumers!))

  (stop [this]
    (protocols.sqs/stop-consumers! this)
    (dissoc this :queues :endpoint))

  protocols.sqs/Consumer
  (start-consumers! [this]
    (start-consumers! this))

  (stop-consumers! [this]
    (stop-consumers! (:queues this))))

(defrecord Producer [config queues-settings]
  component/Lifecycle
  (start [this]
    (-> (assoc this :endpoint (protocols.config/get-in-maybe config [:sqs :endpoint]))
        (gen-queue-map)))

  (stop [this]
    (dissoc this :queues :endpoint))

  protocols.sqs/Producer
  (produce! [this produce-map]
    (produce! this (get-queue this produce-map) produce-map)))


(defn new-consumer [queues-settings]
  (map->Consumer {:queues-settings queues-settings}))

(defn new-producer [queues-settings]
  (map->Producer {:queues-settings queues-settings}))


