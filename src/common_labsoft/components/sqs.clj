(ns common-labsoft.components.sqs
  (:require [clojure.core.async :as async]
            [amazonica.aws.sqs :as sqs]
            [cheshire.core :as cheshire]
            [io.pedestal.log :as log]
            [common-labsoft.misc :as misc]
            [common-labsoft.time]
            [common-labsoft.protocols.sqs :as protocols.sqs]
            [com.stuartsierra.component :as component]
            [common-labsoft.schema :as schema]))

(defn receive-messages! [queue]
  (some->> (sqs/receive-message (:url queue))
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

(defn fetch-message! [queue queue-channel]
  (async/go-loop []
    (doseq [message (receive-messages! queue)]
      (async/>! queue-channel message))
    (recur)))

(defn handle-message! [{handler-fn :handler schema :schema :as queue} queue-channel]
  (async/go-loop []
    (when-let [message (async/<! queue-channel)]
      (try
        (some-> message
                (parse-message schema)
                handler-fn)
        (sqs/delete-message message)
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

(defn create-consumer-loop! [queue]
  (let [queue-channel (async/chan 50)]
    (fetch-message! queue queue-channel)
    (handle-message! queue queue-channel)
    (assoc queue :chan queue-channel)))

(defn init-async-consumer! [queue]
  (if (is-consumer? queue)
    (create-consumer-loop! queue)
    queue))

(defn stop-async-consumer! [queue]
  (when (consumer-started? queue)
    (async/close! (:chan queue)))
  (dissoc! queue :chan))

(defn start-consumers! [queues]
  (misc/map-vals init-async-consumer! queues))

(defn stop-consumers! [queues]
  (misc/map-vals stop-async-consumer! queues))

(defn find-or-create-queue! [qname]
  (or (sqs/find-queue qname)
      (:queue-url (sqs/create-queue qname))))

(defn queue-config->queue [[qname qconf]]
  [qname (assoc qconf :name name
                      :url (find-or-create-queue! (name qname)))])

(defn gen-queue-map [settings-map]
  (into {} (map queue-config->queue settings-map)))

(defn produce! [queue {message :message schema :schema}]
  (if (is-producer? queue)
    (sqs/send-message (:url queue) (serialize-message message schema))
    (log/error :error :producing-to-non-producer-queue :queue queue)))

(defrecord SQS [config queues-settings]
  component/Lifecycle
  (start [this]
    (assoc this :queues (-> (gen-queue-map queues-settings)
                            (start-consumers!))))

  (stop [this]
    (stop-consumers! (:queues this))
    (dissoc this :queues))

  protocols.sqs/SQS
  (produce! [this produce-map]
    (produce! (get-queue this produce-map) produce-map)))

(defn new-sqs [queues-settings]
  (map->SQS {:queues-settings queues-settings}))