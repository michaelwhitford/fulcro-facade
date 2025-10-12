(ns us.whitford.facade.components.save-middleware
  (:require
    [com.fulcrologic.rad.blob :as blob]
    [com.fulcrologic.rad.database-adapters.datomic-cloud :as datomic]
    [com.fulcrologic.rad.middleware.save-middleware :as r.s.middleware]
    [us.whitford.facade.model-rad.attributes :refer [all-attributes]]))

(defn wrap-exceptions-as-form-errors
  ([handler]
   (fn [pathom-env]
     (try
       (let [result (handler pathom-env)]
         result)
       (catch Throwable t
         {:com.fulcrologic.rad.form/errors [{:message (str "Error from form: " (ex-message t))}]})))))

(def middleware
  (->
    (datomic/wrap-datomic-save)
    (wrap-exceptions-as-form-errors)
    (blob/wrap-persist-images all-attributes)
    ;; This is where you would add things like form save security/schema validation/etc.
    ;; This middleware lets you morph values on form save
    (r.s.middleware/wrap-rewrite-values)))
