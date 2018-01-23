(ns common-labsoft.components.http-client
  (:require [common-labsoft.protocols.config :as protocols.config]
            [common-labsoft.protocols.http-client :as protocols.http-client]
            [common-labsoft.protocols.token :as protocols.token]
            [common-labsoft.adapt :as adapt]
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
  (let [auth-payload {:auth/service service-name :auth/password service-password}
        config (:config token-component)
        services (protocols.config/get! config :services)]
    (if (protocols.token/verify token-component token)
      token
      (-> (render-route services :auth :service-token)
          (client/post {:form-params auth-payload :content-type :json})
          :body
          (cheshire/parse-string true)
          :token/jwt))))

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

(defn transform-method
  "Transforms request's method"
  [method data]
  (assoc data :method method))

(defn internalize-response
  [req resp]
  (let [json-or-edn (if (= (get-in resp [:headers :content-type]) :application/json)
                      :json
                      :edn)]
    (adapt/internalize json-or-edn (:body resp) (:schema-resp req)))
  )

(defn externalize-request
  [{:keys [content-type body schema-req] :as req}]
  (assoc req :body (adapt/externalize content-type body schema-req)))

(defrecord HttpClient
  [config token]
  protocols.http-client/HttpClient
  (get-hosts [this] (protocols.config/get! config :services))
  (raw-req! [this data] (client/request data))
  (authd-req! [this data transform]
    (let [service-name (protocols.config/get! config :service-name)
          service-password (protocols.config/get! config :service-password)
          valid-token (resolve-token this token service-name service-password)]
      (if (not= valid-token @token)
        (swap! token (fn [t] (atom valid-token))))
      (let [req-defaults {:headers {:authorization @token} :content-type :json}
            req-data (merge req-defaults data)
            trans-req-data (externalize-request (transform req-data))
            raw-resp (protocols.http-client/raw-req! this trans-req-data)
            resp (internalize-response trans-req-data raw-resp)
            ] resp)))
  (authd-req! [this data]
    (protocols.http-client/authd-req! this data identity))
  (authd-get! [this data]
    (protocols.http-client/authd-req! this data (partial transform-method :get)))
  (authd-post! [this data]
    (protocols.http-client/authd-req! this data (partial transform-method :post)))
  (authd-patch! [this data]
    (protocols.http-client/authd-req! this data (partial transform-method :patch)))
  (authd-delete! [this data]
    (protocols.http-client/authd-req! this data (partial transform-method :delete)))
  (authd-put! [this data]
    (protocols.http-client/authd-req! this data (partial transform-method :put))))


(defn new-http-client
  ([config token]
    (map->HttpClient {:config config :token token}))
  ([token]
   (map->HttpClient {:token token}))
  ([]
   (map->HttpClient {:token (atom "invalid-token")})))
