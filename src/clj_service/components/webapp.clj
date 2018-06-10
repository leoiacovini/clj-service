(ns clj-service.components.webapp
  (:require [com.stuartsierra.component :as component]))

(defrecord WebApp []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defn new-webapp []
  (map->WebApp {}))
