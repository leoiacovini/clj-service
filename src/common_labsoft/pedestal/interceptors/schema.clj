(ns common-labsoft.pedestal.interceptors.schema
  (:require [common-labsoft.schema :as schema]))

(defn coerce [schema]
  {:name  ::coerce
   :enter (fn [context]
            (if-let [body (-> context :request :json-params)]
              (do
                (assoc-in context [:request :data] (schema/coerce body schema)))
              context))})

(def coerce-output
  {:name  ::coerce-output
   :leave (fn [context]
            (if-let [schema (-> context :response :schema)]
              (update-in context [:response :body] #(schema/coerce % schema))
              context))})
