(ns common-labsoft.components.config
  (:require [common-labsoft.protocols.config :as protocols.config]
            [cheshire.core :as cheshire]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]))

;; TODO: Add more logic to the reader, merge from ENV var some options for overrides
(defrecord Config [filename]
  protocols.config/Config
  (get-maybe [this k]
    (get (:conf this) k))

  (get! [this k]
    (or (protocols.config/get-maybe this k)
        (throw (ex-info "ConfigKeyNotFound" {:type :not-found}))))

  (get-in-maybe [this ks]
    (get-in (:conf this) ks))

  (get-in! [this ks]
    (or (protocols.config/get-in-maybe this ks)
        (throw (ex-info "ConfigKeyNotFound" {:type :not-found}))))

  component/Lifecycle
  (start [this]
    (let [conf (-> (io/resource filename)
                   (slurp)
                   (cheshire/parse-string true))]
      (assoc this :conf conf)))

  (stop [this]
    (dissoc this :conf)))

(defn new-config [filename]
  (->Config filename))

