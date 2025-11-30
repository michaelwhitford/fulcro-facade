(ns us.whitford.facade.components.parser
  (:require
   [com.fulcrologic.rad.attributes :as attr]
   [com.fulcrologic.rad.blob :as blob]
   [com.fulcrologic.rad.database-adapters.datomic-common :as common]
   [com.fulcrologic.rad.form :as form]
   [com.fulcrologic.rad.pathom3 :as pathom3]
   [com.fulcrologic.rad.type-support.date-time :as dt]
   [com.wsscode.pathom3.connect.operation :as pco]
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
   [us.whitford.facade.model.entity :as m.entity]
   [us.whitford.facade.model.hpapi :as m.hpapi]
   [us.whitford.facade.model.ipapi :as m.ipapi]
   [us.whitford.facade.model.swapi :as m.swapi]
   [us.whitford.facade.model.wttr :as m.wttr]
   [us.whitford.facade.model.agent-comms :as m.agent-comms]
   [us.whitford.facade.model.prompt :as m.prompt]
   [us.whitford.fulcro-radar.api :as radar]))

(defn normalize-query-params
  "Normalizes query-params by adding simple keyword versions of namespaced keys.
   For example, if query-params contains {:my.ns/page 2}, this will add {:page 2}
   while preserving the original namespaced key. This allows resolvers to use
   simple keywords like (let [{:keys [page]} query-params] ...) regardless of
   how the client sent the params."
  [query-params]
  (reduce-kv
    (fn [m k v]
      (let [simple-key (keyword (name k))]
        ;; Add simple key version, but don't override if simple key already exists
        (cond-> m
          (not (contains? m simple-key)) (assoc simple-key v))))
    query-params
    query-params))

(defn wrap-normalized-query-params
  "Middleware that normalizes query-params in the Pathom environment.
   This allows resolvers to destructure params with simple keywords even when
   the client sends namespaced keywords (e.g., from RAD report controls)."
  [env-middleware]
  (fn [env]
    (let [base-env (env-middleware env)]
      (if-let [qp (:query-params base-env)]
        (assoc base-env :query-params (normalize-query-params qp))
        base-env))))

(def all-resolvers
  "The list of all hand-written resolvers/mutations."
  [m.account/resolvers
   m.entity/resolvers
   m.hpapi/resolvers
   m.ipapi/resolvers
   m.swapi/resolvers
   m.wttr/resolvers
   m.agent-comms/resolvers
   m.prompt/resolvers])

(pco/defresolver form-errors-resolver
  "Provides a default empty vector for ::form/errors when not otherwise provided.
   This prevents 'attribute unreachable' errors when RAD forms load entities.
   Actual form errors from save operations will override this via mutation responses."
  [_ _]
  {::pco/output [::form/errors]}
  {::form/errors []})

(defstate parser
  :start
  (let [env-middleware (-> (attr/wrap-env all-attributes)
                           (form/wrap-env save/middleware delete/middleware)
                           (common/wrap-env (fn [env] {:production (:main datomic-connections)}) d/db)
                           (blob/wrap-env bs/temporary-blob-store {:files bs/file-blob-store})
                           (radar/wrap-env {:ui-ns 'us.whitford.facade.ui.root})
                           ;; Normalize query-params so resolvers can use simple keywords
                           ;; even when RAD controls send namespaced params
                           (wrap-normalized-query-params))]
    (pathom3/new-processor config env-middleware []
                           [form-errors-resolver  ; Must be first so it's available during resolution
                            automatic-resolvers
                            form/resolvers
                            (blob/resolvers all-attributes)
                            radar/resolvers
                            all-resolvers])))
