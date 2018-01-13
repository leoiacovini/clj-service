(ns common-labsoft.pedestal.interceptors.auth
  (:require [clojure.string :as str]
            [common-labsoft.protocols.token :as protocols.token]
            [io.pedestal.log :as log]
            [common-labsoft.exception :as exception]))

(defn headers->token [headers]
  (some-> headers (get "authorization") (str/split #"Bearer ") second))

(defn allowed-scope? [scopes required] (some #((set required) %) scopes))

(def auth
  {:name  ::auth
   :enter (fn [context]
            (if-let [claim (some->> context
                                    :request
                                    :headers
                                    headers->token
                                    (protocols.token/decode (-> context :request :components :token)))]
              (assoc-in context [:request :identity] claim)
              context))})

(defn allow? [& scopes]
  {:name  ::allow
   :enter (fn [context]
            (if (-> (get-in context [:request :identity :scopes])
                    (allowed-scope? scopes))
              context
              (do
                (log/error :log :forbidden-access)
                (exception/forbidden!))))})
