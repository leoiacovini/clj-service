(defproject labsoft-2018/common-labsoft "0.1.0-SNAPSHOT"
  :description "common code for labsoft 2 microservices"
  :url ""
  :license {}
  :plugins [[lein-midje "3.2.1"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clj-http "3.7.0"]
                 [com.datomic/datomic-free "0.9.5651"]
                 [com.cemerick/bandalore "0.0.6" :exclusions [joda-time]]
                 [prismatic/schema "1.1.7"]
                 [cheshire "5.8.0"]
                 [metosin/schema-tools "0.9.1"]
                 [buddy "2.0.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [io.pedestal/pedestal.service "0.5.3"]
                 [io.pedestal/pedestal.jetty "0.5.3"]
                 [ch.qos.logback/logback-classic "1.1.8" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.22"]
                 [org.slf4j/jcl-over-slf4j "1.7.22"]
                 [org.slf4j/log4j-over-slf4j "1.7.22"]
                 [mvxcvi/puget "1.0.2"]
                 [midje "1.9.1"]])
