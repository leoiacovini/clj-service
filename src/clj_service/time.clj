(ns clj-service.time
  (:require [schema.core :as s]
            [cheshire.custom]
            [cheshire.generate]
            [java-time])
  (:import [java.io Writer]))

(def LocalDateTime java.time.LocalDateTime)
(def LocalDate java.time.LocalDate)

(s/defn coerce-to-local-date-time :- LocalDateTime
  [v]
  (java-time/local-date-time v))

(s/defn coerce-to-local-date :- LocalDate
  [v]
  (java-time/local-date v))

(s/defn local-date-time->inst :- java.util.Date
  [date-time :- LocalDateTime]
  (java-time/java-date date-time))

(s/defn local-date->inst :- java.util.Date
  [date :- LocalDate]
  (java-time/java-date date))

(s/defn inst->local-date :- LocalDate
  [date-time :- LocalDateTime]
  (coerce-to-local-date date-time))

(s/defn inst->local-date-time :- LocalDateTime
  [date :- LocalDate]
  (coerce-to-local-date-time date))

(s/defn str->local-date-time :- LocalDateTime
  [str :- s/Str]
  (coerce-to-local-date-time str))

(s/defn str->local-date :- LocalDate
  [str :- s/Str]
  (coerce-to-local-date str))

(s/defn now :- LocalDateTime [] (java-time/local-date-time))
(s/defn today :- LocalDate [] (java-time/local-date))

(cheshire.generate/add-encoder LocalDateTime (fn [val writer]
                                               (cheshire.generate/encode-str (str val) writer)))

(cheshire.generate/add-encoder LocalDate (fn [val writer]
                                           (cheshire.generate/encode-str (str val) writer)))

(defmethod print-dup LocalDateTime [datetime ^Writer out]
  (.write out (str "#time/date-time \"" datetime \")))

(defmethod print-method LocalDateTime [datetime ^Writer out]
  (.write out (str "#time/date-time \"" datetime \")))

(defmethod print-dup LocalDate [date ^Writer out]
  (.write out (str "#time/local-date \"" date \")))

(defmethod print-method LocalDate [date ^Writer out]
  (.write out (str "#time/local-date \"" date \")))
