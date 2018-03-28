(ns common-labsoft.components.http-client
  (:require [common-labsoft.protocols.config :as protocols.config]
            [common-labsoft.protocols.http-client :as protocols.http-client]
            [common-labsoft.protocols.token :as protocols.token]
            [common-labsoft.fault-tolerance :as fault-tolerance]
            [common-labsoft.adapt :as adapt]
            [clj-http.client :as client]
            [com.stuartsierra.component :as component]
            [common-labsoft.exception :as exception]))

(defn- resolve-replace-map
  [template-str replace-map]
  (reduce-kv (fn [s k v] (clojure.string/replace s (str k) (str v))) template-str replace-map))

(defn render-route
  "Resolves to a full url"
  ([hosts service-host endpoint replace-map]
   (let [service (get hosts service-host)
         endpoints (get service :endpoints)]
     (resolve-replace-map (str (get service :host) (get endpoints endpoint)) replace-map)))
  ([hosts service-host endpoint]
   (render-route hosts service-host endpoint {})))

(defn render-keyworded-url
  "Resolves :customers/one-customer to its url"
  [hosts key replace-map]
  (let [service-host (keyword (namespace key))
        endpoint (keyword (name key))]
    (render-route hosts service-host endpoint replace-map)))

(defn verify-token [token token-component]
  (when (and token (protocols.token/verify token-component token))
    token))

(defn request-new-token [config auth-payload]
  (-> (protocols.config/get! config :services)
      (render-route :auth :service-token)
      (client/post {:form-params  auth-payload
                    :content-type :json})
      :body
      adapt/from-json
      :token/jwt))

(defn auth-payload [name pass]
  {:auth/service name :auth/password pass})

(defn resolve-token
  "Receives a token. If its valid, returns it. If its not, generate a valid token and return it."
  [{:keys [service-name service-password service-token token config]}]
  (or (verify-token @service-token token)
      (->> (auth-payload service-name service-password)
           (request-new-token config)
           (reset! service-token))))

;; Transform methods: receives and returns request's data.
(defn transform-url
  "Generates a url if the data was provided with host/endpoint or keyworded url. Applies replace-map"
  [{:keys [host endpoint replace-map url] :as data} hosts]
  (if (keyword? url)
    (let [url (render-keyworded-url hosts url (or replace-map {}))]
      (merge (dissoc data :replace-map) {:url url}))
    (if (and (some? host) (some? endpoint))
      (merge (dissoc data :replace-map :endpoint :host) {:url (render-route hosts host endpoint (or replace-map {}))})
      data)))

(defn internalize-response
  [req resp]
  (let [json-or-edn (if (= (get-in resp [:headers "Content-Type"]) "application/json")
                      :json
                      :edn)]
    (adapt/internalize json-or-edn (:body resp) (:schema-resp req))))

(defn externalize-request
  [{:keys [content-type body schema-req] :as req}]
  (assoc req :body (adapt/externalize content-type body schema-req)))

(defn build-req
  [token req-data hosts]
  (-> {:headers {:authorization token} :content-type :json}
      (merge req-data)
      externalize-request
      (transform-url hosts)))

(defrecord HttpClient [config token]
  component/Lifecycle
  (start [this]
    (assoc this :service-token (atom nil)
                :service-name (protocols.config/get! config :service-name)
                :service-password (protocols.config/get! config :service-password)))

  (stop [this]
    (dissoc this :service-token :service-name :service-password))

  protocols.http-client/HttpClient
  (raw-req! [this data]
    (fault-tolerance/with-retries 3
      (client/request data)))

  (authd-req! [this data]
    (let [req (-> (resolve-token this)
                  (build-req data (protocols.config/get! config :services)))]
      (->> (protocols.http-client/raw-req! this req)
           (internalize-response req)))))

(defn new-http-client
  ([config]
   (map->HttpClient {:config config}))
  ([]
   (map->HttpClient {})))
