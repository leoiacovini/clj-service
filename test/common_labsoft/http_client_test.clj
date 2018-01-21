(ns common-labsoft.http-client-test
  (:require [midje.sweet :refer :all]
            [com.stuartsierra.component :as component]
            [common-labsoft.components.http-client :refer :all]
            [common-labsoft.protocols.http-client :refer :all]
            [common-labsoft.components.config :as config]
            [common-labsoft.components.token :as token]))

(def test-config {:customers {:host "https://customers.labsoft.host"
                              :endpoints { :one-customer "/customers/:id"
                                           :all-customers "/customers" }}
                  })

(def configr (component/start (config/new-config "test_config.json")))
;;(def tokenr (token/->Token configr s3-auth))
(def http (new-http-client configr (atom {})))              ;; TODO: fix

(fact "get-hosts must returns map with services hosts and endpoints"
      (get-hosts http) => test-config)
;
(fact "when rendering routes"
      (fact "create a full url even without replace-map"
            (render-route test-config :customers :all-customers) => "https://customers.labsoft.host/customers")
      (fact "create a url based on replace-map"
            (render-route test-config :customers :one-customer {:id 42}) => "https://customers.labsoft.host/customers/42"))

(fact "when performing a request"
      (fact "must be able to do a raw request"
            (:status (raw-req! http {:method :get :url "https://google.com"})) => 200))
