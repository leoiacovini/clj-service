(ns common-labsoft.components.http-client
  (:require [common-labsoft.protocols.config :as protocols.config]
            [common-labsoft.protocols.http-client :as protocols.http-client]
            [common-labsoft.protocols.token :as protocols.token]
            [common-labsoft.components.token :as components.token]
            [common-labsoft.adapt :as adapt]
            [clj-http.client :as client]
            [cheshire.core :as cheshire]
            [com.stuartsierra.component :as component]))

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
  (when (protocols.token/verify token-component token)
    token))

(defn resolve-token
  "Receives a token. If its valid, returns it. If its not, generate a valid token and return it."
  [token-component token* service-name service-password]
  (let [auth-payload {:auth/service service-name :auth/password service-password}
        config (:config token-component)
        services (protocols.config/get! config :services)]
    (or (verify-token @token* token-component)
        (-> (render-route services :auth :service-token)
            (client/post {:form-params auth-payload :content-type :json})
            :body
            (cheshire/parse-string true)
            :token/jwt
            (->> (reset! token*))))))

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
    (adapt/internalize json-or-edn (:body resp) (:schema-resp req)))
  )

(defn externalize-request
  [{:keys [content-type body schema-req] :as req}]
  (assoc req :body (adapt/externalize content-type body schema-req)))

(defn build-req
  [token req-data hosts]
  (-> {:headers {:authorization token} :content-type :json}
      (merge req-data)
      externalize-request
      (transform-url hosts)))

(defrecord HttpClient
  [config token s3-auth]
  protocols.http-client/HttpClient
  (get-hosts [this] (protocols.config/get! config :services))
  (raw-req! [this data] (client/request data))
  (authd-req! [this data]
    (let [service-name (protocols.config/get! config :service-name)
          service-password (protocols.config/get! config :service-password)
          token-component (component/start (components.token/map->Token {:config config :s3-auth s3-auth}))
          req (->  (resolve-token token-component token service-name service-password)
                   (build-req data (protocols.http-client/get-hosts this)))]
      (->> (protocols.http-client/raw-req! this req)
           (internalize-response req)))))


(defn new-http-client
  ([config token s3-auth]
    (map->HttpClient {:config config :token token :s3-auth s3-auth}))
  ([token]
   (map->HttpClient {:token token}))
  ([]
   (map->HttpClient {:token (atom nil)})))
