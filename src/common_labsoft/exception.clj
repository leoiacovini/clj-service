(ns common-labsoft.exception)

(defn- throw-ex [name type code] (throw (ex-info name {:type type :code code :message name})))

(defn bad-request! [] (throw-ex "BadRequest" :bad-request 400))
(defn unathorized! [] (throw-ex "Unauthorized" :unauthorized 401))
(defn forbidden! [] (throw-ex "Forbidden" :forbidden 403))
(defn not-found! [] (throw-ex "NotFound" :not-found 404))
(defn server-error! [] (throw-ex "ServerError" :server-error 500))