(ns development
  (:require
    [clojure.tools.namespace.repl :as tools-ns :refer [set-refresh-dirs]]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [com.fulcrologic.rad.type-support.date-time :as dt]
    [datomic.client.api :as d]
    [mount.core :as mount]
    [taoensso.timbre :as log]
    [us.whitford.facade.components.database :refer [datomic-connections]]
    [us.whitford.facade.components.ring-middleware]
    [us.whitford.facade.components.server]
    [us.whitford.facade.model.account :refer [new-account]]))

;; Prevent tools-ns from findinga source in other places, such as resources
(set-refresh-dirs "src/main" "src/dev")

(comment
  (let [db (d/db (:main datomic-connections))]
    (d/pull db '[*] [:account/id (new-uuid 001)])))

(defn seed! []
  (dt/set-timezone! "America/Phoenix")
  (let [connection (:main datomic-connections)]
    (when connection
          (log/info "SEEDING data.")
          (d/transact connection {:tx-data [(new-account "michael@whitford.us")]})
          )))

(defn start []
  (mount/start-with-args {:config "config/dev.edn"})
  (seed!)
  :ok)

(defn stop
  "Stop the server."
  []
  (mount/stop))

(defn fast-restart
  "Stop, refresh, and restart the server."
  []
  (stop)
  (start))

(defn restart
  "Stop, refresh, and restart the server."
  []
  (stop)
  (tools-ns/refresh :after 'development/start))

(comment
  (restart))
