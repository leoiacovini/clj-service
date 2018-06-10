(ns clj-service.pedestal.interceptors.schema
  (:require [schema.core]
            [clj-service.schema :as schema]))

(defn coerce [schema]
  {:name  ::coerce
   :enter (fn [context]
            (let [body (or (-> context :request :json-params)
                           (-> context :request :edn-params)
                           (-> context :request :transit-params))]
              (assoc-in context [:request :data] (schema/coerce body schema))))})

(def coerce-output
  {:name  ::coerce-output
   :leave (fn [context]
            (if-let [schema (-> context :response :schema)]
              (update-in context [:response :body] #(schema/coerce % schema))
              context))})
