(ns common-labsoft.fault-tolerance)

(defn try-times
  [n f]
  (loop [n n]
    (if-let [result (try
                      (vector (f))
                      (catch Exception e
                        (when (zero? n)
                          (throw e))))]
      (first result)
      (recur (dec n)))))

(defmacro with-retries
  [n & body]
  `(try-times ~n (fn [] ~@body)))
