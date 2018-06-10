(ns clj-service.misc
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [puget.printer :as puget])
  (:import (java.util UUID)))

(def custom-color "\033[37;1;45m\033[1m")
(def no-color "\033[0m")

(defn uuid [] (UUID/randomUUID))

(defn str->uuid [s] (UUID/fromString s))

(defn map-vals [f m] (into {} (map (fn [[k v]] [k (f v)]) m)))

(defn map-keys [f m] (into {} (map (fn [[k v]] [(f k) v]) m)))

(defn underscore-dash-keyword [keyw]
  (let [_->-  (fn [v] (some-> v name (str/replace "_" "-")))
        ns    (namespace keyw)
        k     (name keyw)]
    (keyword (_->- ns) (_->- k))))

(defn dash-underscore-keyword [keyw]
  (let [_->-  (fn [v] (some-> v name (str/replace "-" "_")))
        ns    (namespace keyw)
        k     (name keyw)]
    (keyword (_->- ns) (_->- k))))

(defn underscore->dash [m]
  (let [f (fn [[k v]] (if (keyword? k) [(underscore-dash-keyword k) v] [k v]))]
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(defn dash->underscore [m]
  (let [f (fn [[k v]] (if (keyword? k) [(dash-underscore-keyword k) v] [k v]))]
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(defn with-color [text] (str custom-color text no-color))

(defn pretty-print [x]
  (let [str (with-out-str (puget/cprint x {:print-fallback :pretty
                                           :seq-limit      10
                                           :map-delimiter  ""}))]
    (subs str 0 (dec (count str)))))

(defn debug [form]
  `(let [res# ~form]
     (println
       (with-color " debug ")
       (pretty-print '~form)
       (with-color "=>\n")
       (pretty-print res#)
       (with-color "<="))
     res#))

(defn assoc-if [m k v] (if v (assoc m k v) m))
