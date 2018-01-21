(ns common-labsoft.components.http-client
  (:require [common-labsoft.protocols.config :as protocols.config]
            [common-labsoft.protocols.http-client :as protocols.http-client]
            [common-labsoft.protocols.token :as protocols.token]
            [clj-http.client :as client]))

(defn resolve-token
  "Receives a token. If its valid, returns it. If its not, generate a valid token and return it."
  [token]
  (let [token (:token @token)
        record (:record @token)
        content (:content @token)]
    (if (protocols.token/verify record token)
      token
      (protocols.token/encode record content))))

(defrecord HttpClient
  [config token] ;"This token is a atom with keys :record :content :token"
  protocols.http-client/HttpClient
  (get-hosts [this] (protocols.config/get! config :services))
  (raw-req! [this data] (client/request data))
  (authd-req! [this data]
    (let [valid-token (resolve-token token)]
      (if (not= valid-token token)
        (swap! token (fn [t] (merge @t {:token valid-token}))))
      (protocols.http-client/raw-req! this (merge data {:headers {:authorization (str "Bearer " (:token @token))}}))))
  )

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
    (render-route hosts service-host endpoint {}))
  )


(defn new-http-client [config token]
  (map->HttpClient {:config config :token token}))