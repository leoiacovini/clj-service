(ns common-labsoft.components.http-client
  (:require [common-labsoft.protocols.config :as protocols.config]
            [common-labsoft.protocols.http-client :as protocols.http-client]
            [common-labsoft.protocols.token :as protocols.token]
            [clj-http.client :as client]
            [cheshire.core :as cheshire]))



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

(defn resolve-token
  "Receives a token. If its valid, returns it. If its not, generate a valid token and return it."
  [token-component token service-name service-password]
  (let [auth-payload {:auth/service-name service-name :auth/auth-password service-password}
        config (:config token-component)
        services (protocols.config/get! config :services)]
    (if (protocols.token/verify token-component token)
      token
      (-> (render-route services :auth :service-token)
          (client/post {:form-params auth-payload :content-type :json})
          (:body)
          (cheshire/parse-string true)
          (:token)))))

;; Transform methods: receives and returns request's data.
(defn transform-url
  "Generates a url if the data was provided with host/endpoint or keyworded url. Applies replace-map"
  [data hosts]
  (let [service (:host data)
        endpoint (:endpoint data)
        replace-map (:replace-map data {})
        url (:url data)]
    (if (keyword? url)
      (let [url (render-keyworded-url hosts url (:replace-map data {}))]
        (merge (dissoc data :replace-map) {:url url}))
      (if (and (some? service) (some? endpoint))
        (merge (dissoc data :replace-map :endpoint :host) {:url (render-route hosts service endpoint replace-map)})
        data))))

(defn- transform-method
  "Transforms request's method"
  [data method]
  (merge data {:method method}))

(defrecord HttpClient
  [config token] ;"This token is a atom with keys :record :content :token"
  protocols.http-client/HttpClient
  (get-hosts [this] (protocols.config/get! config :services))
  (raw-req! [this data] (client/request data))
  (authd-req! [this data]
    (let [service-name (protocols.config/get! config :service-name)
          service-password (protocols.config/get! config :service-password)
          valid-token (resolve-token this token service-name service-password)]
      (if (not= valid-token token)
        (swap! token (fn [t] (merge @t {:token valid-token}))))
      (protocols.http-client/raw-req! this (merge {:headers {:authorization (str "Bearer " (:token @token))}} data))))
  )

(defn new-http-client [config token]
  (map->HttpClient {:config config :token token}))