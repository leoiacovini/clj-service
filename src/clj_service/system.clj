(ns clj-service.system
  (:require [com.stuartsierra.component :as component]))

(def system (atom nil))

(defn bootstrap! [system-map]
  (prn "Starting system!!")
  (let [new-system (-> system-map
                       component/start-system)]
    (prn "System started successfully!!")
    (reset! system new-system)))

(defn stop! []
  (component/stop-system @system)
  (reset! system nil))
