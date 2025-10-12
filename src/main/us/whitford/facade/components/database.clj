(ns us.whitford.facade.components.database
  (:require
    [com.fulcrologic.rad.database-adapters.datomic-cloud :as datomic]
    [mount.core :refer [defstate]]
    [us.whitford.facade.components.config :refer [config]]
    [us.whitford.facade.model-rad.attributes :refer [all-attributes]]))

(defstate ^{:on-reload :noop} datomic-connections
  :start
  (datomic/start-databases all-attributes config))
