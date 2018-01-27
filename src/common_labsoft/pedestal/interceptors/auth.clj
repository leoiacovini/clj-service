(ns common-labsoft.pedestal.interceptors.auth
  (:require [clojure.string :as str]
            [common-labsoft.protocols.token :as protocols.token]
            [io.pedestal.log :as log]
            [common-labsoft.datomic.api]
            [common-labsoft.exception :as exception]
            [datomic.api :as d]))

(defn headers->token [headers]
  (some-> headers (get "authorization") (str/split #"Bearer ") second))

(defn allowed-scope? [scopes required] (some #((set required) %) scopes))

(defn token-component [context] (-> context :request :components :token))
(defn datomic-component [context] (-> context :request :components :datomic))

(defn forbidden []
  (log/error :log :forbidden-access)
  (exception/forbidden! {}))

(def auth
  {:name  ::auth
   :enter (fn [context]
            (if-let [claim (some->> context
                                    :request
                                    :headers
                                    headers->token
                                    (protocols.token/decode (token-component context)))]
              (assoc-in context [:request :identity] claim)
              context))})

(defn allow-scopes? [& scopes]
  {:name  ::allow-scopes?
   :enter (fn [context]
            (if (-> (get-in context [:request :identity :token/scopes])
                    (allowed-scope? scopes))
              context
              (forbidden)))})

(defn allow-types? [& types]
  {:name  ::allow-types?
   :enter (fn [context]
            (if (-> (get-in context [:request :identity :token/type])
                    ((set types)))
              context
              (forbidden)))})

(defn owner? [res-key query]
  {:name  ::owner?
   :enter (fn [context]
            (let [auth-id (get-in context [:request :identity :token/sub])
                  res-id (get-in context [:request :path-params res-key])
                  datomic (datomic-component context)]
              (if (d/q {:find  '[?entity .]
                        :in    '[$ ?owner-id ?resource-id]
                        :where query}
                       (common-labsoft.datomic.api/db datomic)
                       auth-id
                       res-id)
                context
                (forbidden))))})

(defn user-identity [path-id]
  {:name  :user-identity
   :enter (fn [context]
            (let [request-id (str (get-in context [:request :path-params path-id]))
                  auth-id (str (get-in context [:request :identity :token/sub]))]
              (if (= request-id auth-id)
                context
                (forbidden))))})
