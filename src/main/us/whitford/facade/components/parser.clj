(ns us.whitford.facade.components.parser
  (:require
   [com.fulcrologic.rad.attributes :as attr]
   [com.fulcrologic.rad.blob :as blob]
   [com.fulcrologic.rad.database-adapters.datomic-common :as common]
   [com.fulcrologic.rad.form :as form]
   [com.fulcrologic.rad.pathom3 :as pathom3]
   [com.fulcrologic.rad.type-support.date-time :as dt]
   [datomic.client.api :as d]
   [mount.core :refer [defstate]]
   [us.whitford.facade.components.auto-resolvers :refer [automatic-resolvers]]
   [us.whitford.facade.components.blob-store :as bs]
   [us.whitford.facade.components.config :refer [config]]
   [us.whitford.facade.components.database :refer [datomic-connections]]
   [us.whitford.facade.components.delete-middleware :as delete]
   [us.whitford.facade.components.save-middleware :as save]
   [us.whitford.facade.model-rad.attributes :refer [all-attributes]]
   ;; Require namespaces that define resolvers
   [us.whitford.facade.model.account :as m.account]
   [us.whitford.facade.model.hpapi :as m.hpapi]
   [us.whitford.facade.model.swapi :as m.swapi]))

(def all-resolvers
  "The list of all hand-written resolvers/mutations."
  [m.account/resolvers
   m.hpapi/resolvers
   m.swapi/resolvers])

(defstate parser
  :start
  (let [env-middleware (-> (attr/wrap-env all-attributes)
                           (form/wrap-env save/middleware delete/middleware)
                           (common/wrap-env (fn [env] {:production (:main datomic-connections)}) d/db)
                           (blob/wrap-env bs/temporary-blob-store {:files bs/file-blob-store}))]
    (pathom3/new-processor config env-middleware []
                           [automatic-resolvers
                            form/resolvers
                            (blob/resolvers all-attributes)
                            all-resolvers])))
