(ns common-labsoft.components.sqs
  (:require [clojure.core.async :as async]
            [amazonica.aws.sqs :as sqs]
            [cheshire.core :as cheshire]
            [io.pedestal.log :as log]
            [common-labsoft.misc :as misc]
            [common-labsoft.time]
            [common-labsoft.protocols.sqs :as protocols.sqs]
            [com.stuartsierra.component :as component]
            [common-labsoft.schema :as schema]
            [common-labsoft.protocols.config :as protocols.config]))

(defn receive-messages! [endpoint queue]
  (some->> (sqs/receive-message {:endpoint endpoint} (:url queue))
           :messages
           (map #(assoc % :queue-url (:url queue)))))

(defn parse-message [message schema]
  (-> message
      :body
      (cheshire/parse-string true)
      (schema/coerce-if schema)))

(defn serialize-message [message schema]
  (-> (schema/coerce-if message schema)
      (cheshire/generate-string message)))

(defn fetch-message! [endpoint queue queue-channel]
  (async/go-loop []
    (doseq [message (receive-messages! endpoint queue)]
      (async/>! queue-channel message))
    (recur)))

(defn handle-message! [endpoint {handler-fn :handler schema :schema :as queue} queue-channel]
  (async/go-loop []
    (when-let [message (async/<! queue-channel)]
      (try
        (some-> message
                (parse-message schema)
                handler-fn)
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

(defn create-consumer-loop! [endpoint queue]
  (let [queue-channel (async/chan 50)]
    (fetch-message! endpoint queue queue-channel)
    (handle-message! endpoint queue queue-channel)
    (assoc queue :chan queue-channel)))

(defn init-async-consumer! [endpoint queue]
  (if (is-consumer? queue)
    (create-consumer-loop! endpoint queue)
    queue))

(defn stop-async-consumer! [queue]
  (when (consumer-started? queue)
    (async/close! (:chan queue)))
  (dissoc! queue :chan))

(defn start-consumers! [{:keys [queues endpoint] :as this}]
  (update this :queues #(misc/map-vals (partial init-async-consumer! endpoint) %)))

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
    (sqs/send-message {:endpoint endpoint} (:url queue) (serialize-message message schema))
    (log/error :error :producing-to-non-producer-queue :queue queue)))

(defrecord SQS [config queues-settings]
  component/Lifecycle
  (start [this]
    (prn "Starting SQS Client component...")
    (-> (assoc this :endpoint (protocols.config/get-in-maybe config [:sqs :endpoint]))
        (gen-queue-map)
        (start-consumers!)))

  (stop [this]
    (stop-consumers! (:queues this))
    (dissoc this :queues))

  protocols.sqs/SQS
  (produce! [this produce-map]
    (produce! this (get-queue this produce-map) produce-map)))

(defn new-sqs [queues-settings]
  (map->SQS {:queues-settings queues-settings}))
