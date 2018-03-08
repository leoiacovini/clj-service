(ns common-labsoft.http-client-test
  (:require [midje.sweet :refer :all]
            [com.stuartsierra.component :as component]
            [common-labsoft.components.http-client :as components.http-client]
            [common-labsoft.components.s3-client :as components.s3-client]
            [common-labsoft.protocols.http-client :as protocols.http-client]
            [common-labsoft.components.config :as config]
            [common-labsoft.components.token :as components.token]))

(def test-hosts {:customers {:host      "https://customers.labsoft.host"
                             :endpoints {:one-customer  "/customers/:id"
                                         :all-customers "/customers"}}
                 :auth      {:host      "https://putsreq.com"
                             :endpoints {:service-token "/nMNuwB4x0EFEsGdBxkpv"}}
                 :httpbin   {:host      "http://httpbin.org"
                             :endpoints {:get "/get"}}})

(def config (component/start (config/new-config "test_config.json")))
(def s3 (component/start (components.s3-client/map->S3Client {:bucket-config-key :s3-auth :config config})))
(def token (component/start (components.token/map->Token {:config config :s3-auth s3})))
(def http (component/start (components.http-client/map->HttpClient {:config config :token token})))

(fact "when rendering routes"
  (fact "create a full url even without replace-map"
    (components.http-client/render-route test-hosts :customers :all-customers) => "https://customers.labsoft.host/customers")
  (fact "create a url based on replace-map"
    (components.http-client/render-route test-hosts :customers :one-customer {:id 42}) => "https://customers.labsoft.host/customers/42")
  (fact "create a full url from namespaced keywords"
    (components.http-client/render-keyworded-url test-hosts :customers/one-customer {:id 42}) => "https://customers.labsoft.host/customers/42"))

(fact "when performing a request"
  (fact "must be able to do a raw request"
    (:status (protocols.http-client/raw-req! http {:method :get :url "https://google.com"})) => 200))

(fact "data must be transformed"
  (fact "to generate the url with namespaced keyword url"
    (components.http-client/transform-url {:url :customers/one-customer :replace-map {:id 42}} test-hosts) => {:url "https://customers.labsoft.host/customers/42"})
  (fact "to generate the url with host and endpoint"
    (components.http-client/transform-url {:host :customers :endpoint :one-customer :replace-map {:id 42}} test-hosts) => {:url "https://customers.labsoft.host/customers/42"}))

#_(fact "authd-req"
  (fact "must work"
    (protocols.http-client/authd-req! http {:method :get :url :httpbin/get}) => (contains {:url "http://httpbin.org/get"})))
