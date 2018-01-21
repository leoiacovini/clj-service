(ns common-labsoft.http-client-test
  (:require [midje.sweet :refer :all]
            [com.stuartsierra.component :as component]
            [common-labsoft.components.http-client :refer :all]
            [common-labsoft.protocols.http-client :refer :all]
            [common-labsoft.components.config :as config]
            [common-labsoft.components.token :as token]))

(def test-hosts {:customers {:host       "https://customers.labsoft.host"
                              :endpoints { :one-customer "/customers/:id"
                                           :all-customers "/customers" }}
                  })

(def configr (component/start (config/new-config "test_config.json")))
;;(def tokenr (token/->Token configr s3-auth))
(def http (new-http-client configr (atom {})))              ;; TODO: fix

(fact "get-hosts must returns map with services hosts and endpoints"
      (get-hosts http) => test-hosts)
;
(fact "when rendering routes"
      (fact "create a full url even without replace-map"
            (render-route test-hosts :customers :all-customers) => "https://customers.labsoft.host/customers")
      (fact "create a url based on replace-map"
            (render-route test-hosts :customers :one-customer {:id 42}) => "https://customers.labsoft.host/customers/42")
      (fact "create a full url from namespaced keywords"
            (render-keyworded-url test-hosts :customers/one-customer {:id 42}) => "https://customers.labsoft.host/customers/42"))

(fact "when performing a request"
      (fact "must be able to do a raw request"
            (:status (raw-req! http {:method :get :url "https://google.com"})) => 200))

(fact "data must be transformed"
      (fact "to generate the url with namespaced keyword url"
            (transform-url {:url :customers/one-customer :replace-map {:id 42}} test-hosts) => {:url "https://customers.labsoft.host/customers/42"})
      (fact "to generate the url with host and endpoint"
            (transform-url {:host :customers :endpoint :one-customer :replace-map {:id 42}} test-hosts) => {:url "https://customers.labsoft.host/customers/42"}))

