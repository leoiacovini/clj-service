(defproject leoiacovini/clj-service "0.11.0-SNAPSHOT"
  :description "Common code for creating Clojure services using pedestal"
  :url ""
  :deploy-repositories [["clojars" {:url      "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password}]]

  :plugins [[lein-midje "3.2.1"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clojure.java-time "0.3.2"]
                 [aero "1.1.3"]
                 [prismatic/schema "1.1.9"]
                 [metosin/schema-tools "0.10.3"]
                 [cheshire "5.8.0"]
                 [buddy "2.0.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [io.pedestal/pedestal.service "0.5.3"]
                 [io.pedestal/pedestal.jetty "0.5.3"]
                 [ch.qos.logback/logback-classic "1.1.8"]
                 [org.slf4j/log4j-over-slf4j "1.7.22"]
                 [org.slf4j/slf4j-api "1.7.22"]
                 [mvxcvi/puget "1.0.2"]
                 [midje "2.0.0-SNAPSHOT"]]

  :profiles {:uberjar {:aot :all}
             :dev     {:injections [(require 'clj-service.misc)
                                    (require 'clj-service.time)]}})
