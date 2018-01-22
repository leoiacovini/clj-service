(ns common-labsoft.pedestal.interceptors.adapt
  (:require [common-labsoft.adapt :as adapt]
            [io.pedestal.http.content-negotiation :as conneg]))


(def supported-types ["application/json" "application/edn" "text/plain" "text/html"])

(def content-neg-intc (conneg/negotiate-content supported-types))

(defn accepted-type
  [context]
  (prn (get-in context [:request :accept :field]))
  (get-in context [:request :accept :field] "application/json"))

(defn transform-content
  [body content-type]
  (case content-type
    "text/html" body
    "text/plain" body
    "application/edn" (adapt/to-edn body)
    "application/json" (adapt/to-json body)))

(defn coerce-to
  [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

(def coerce-body
  {:name ::coerce-body
   :leave
         (fn [context]
           (if (get-in context [:response :headers "Content-Type"])
             context
             (update-in context [:response] coerce-to (accepted-type context))))})
