(ns clj-service.pedestal.interceptors.adapt
  (:require [clj-service.adapt :as adapt]
            [io.pedestal.http.content-negotiation :as conneg]
            [clj-service.misc :as misc]
            [clj-service.schema :as schema]))

(def supported-types ["application/json" "application/edn" "text/plain" "text/html"])

(def content-neg-intc (conneg/negotiate-content supported-types))

(defn accepted-type
  [context]
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

(defn path->uuid
  ([path-id]
   (path->uuid path-id path-id))
  ([path-id req-key]
   {:name  ::path->uuid
    :enter (fn [context]
             (let [path-uuid (misc/str->uuid (get-in context [:request :path-params path-id]))]
               (assoc-in context [:request req-key] path-uuid)))}))

(defn coerce-path
  ([path-key schema]
   (coerce-path path-key schema path-key))
  ([path-key schema req-key]
   {:name  ::coerce-path
    :enter (fn [context]
             (let [path-val (schema/coerce (get-in context [:request :path-params path-key]) schema)]
               (assoc-in context [:request req-key] path-val)))}))
