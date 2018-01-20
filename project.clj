(defproject labsoft-2018/common-labsoft "0.6.1-SNAPSHOT"
  :description "common code for labsoft 2 microservices"
  :url ""
  :license {}
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username :env/datomic_username
                                   :password :env/datomic_password}}
  :deploy-repositories [["clojars" {:url      "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password}]]
  :injections [(require 'common-labsoft.misc)
               (require 'common-labsoft.time)]
  :plugins [[lein-midje "3.2.1"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clj-http "3.7.0"]
                 [com.datomic/datomic-pro "0.9.5661" :exclusions [org.slf4j/slf4j-nop]]
                 [prismatic/schema "1.1.7"]
                 [cheshire "5.8.0"]
                 [org.clojure/core.async "0.4.474"]
                 [metosin/schema-tools "0.9.1"]
                 [buddy "2.0.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [io.pedestal/pedestal.service "0.5.3"]
                 [io.pedestal/pedestal.jetty "0.5.3"]
                 [ch.qos.logback/logback-classic "1.1.8"]
                 [org.slf4j/log4j-over-slf4j "1.7.22"]
                 [org.slf4j/slf4j-api "1.7.22"]
                 [mvxcvi/puget "1.0.2"]
                 [amazonica "0.3.117"]
                 [midje "1.9.1"]])
