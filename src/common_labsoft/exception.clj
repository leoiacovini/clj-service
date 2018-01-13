(ns common-labsoft.exception)

(defn- throw-ex [name type code details] (throw (ex-info name (merge {:type type :code code :message name} details))))

(defn bad-request! [details] (throw-ex "BadRequest" :bad-request 400 details))
(defn unathorized! [details] (throw-ex "Unauthorized" :unauthorized 401 details))
(defn forbidden! [details] (throw-ex "Forbidden" :forbidden 403 details))
(defn not-found! [details] (throw-ex "NotFound" :not-found 404 details))
(defn server-error! [details] (throw-ex "ServerError" :server-error 500 details))