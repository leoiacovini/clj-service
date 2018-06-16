(ns clj-service.pedestal.interceptors.auth
  (:require [clojure.string :as str]
            [io.pedestal.log :as log]
            [clj-service.exception :as exception]
            [clj-service.protocols.crypto :as protocols.crypto]))

(defn headers->token [headers]
  (some-> headers (get "authorization") (str/split #"Bearer ") second))

(defn allowed-scope? [scopes required] (some #((set required) %) scopes))

(defn forbidden []
  (log/error :log :forbidden-access)
  (exception/forbidden! {}))

(defn auth-scopes? [scopes context]
  (-> (get-in context [:request :identity :scopes])
      (allowed-scope? scopes)))


(defn auth-identity? [path-id context]
  (let [request-id (str (get-in context [:request :path-params path-id]))
        auth-id (str (get-in context [:request :identity :sub]))]
    (= request-id auth-id)))

(def auth
  {:name  ::auth
   :enter (fn [context]
            (if-let [claim (some->> context
                                    :request
                                    :headers
                                    headers->token
                                    (protocols.crypto/jwt-decode (-> context :request :components :crypto)))]
              (assoc-in context [:request :identity] claim)
              (forbidden)))})

(defn allow-scopes? [& scopes]
  {:name  ::allow-scopes?
   :enter (fn [context] (if (auth-scopes? scopes context) context (forbidden)))})

(defn user-identity [path-id]
  {:name  :user-identity
   :enter (fn [context] (if (auth-identity? path-id context) context (forbidden)))})

(defn scopes-or? [scopes or-fn]
  {:name  ::scopes-or?
   :enter (fn [context]
            (if (or (auth-scopes? scopes context)
                    (or-fn context))
              context
              (forbidden)))})
