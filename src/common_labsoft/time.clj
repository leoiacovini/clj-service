(ns common-labsoft.time
  (:require [schema.core :as s]
            [clj-time.coerce :as time.coerce]
            [clj-time.core :as time]
            [clj-time.local :as time.local]))

(def LocalDateTime org.joda.time.DateTime)
(def LocalDate org.joda.time.LocalDate)

(s/defn date-time->inst :- java.util.Date
  [date-time :- LocalDateTime]
  (time.coerce/to-date date-time))

(s/defn local-date->inst :- java.util.Date
  [date :- LocalDate]
  (time.coerce/to-date date))

(s/defn inst->local-date :- LocalDate
  [date-time :- LocalDateTime]
  (time.coerce/to-local-date date-time))

(s/defn inst->date-time :- LocalDateTime
  [date :- LocalDate]
  (time.coerce/to-local-date-time date))

(s/defn now :- LocalDateTime [] (time/now))
(s/defn local-now :- LocalDateTime [] (time.local/local-now))
(s/defn today :- LocalDate [] (time/today))