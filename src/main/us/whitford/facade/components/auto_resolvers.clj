(ns us.whitford.facade.components.auto-resolvers
  (:require
    [com.fulcrologic.rad.database-adapters.datomic-cloud :as datomic]
    [com.fulcrologic.rad.resolvers :as res]
    [mount.core :refer [defstate]]
    [us.whitford.facade.model-rad.attributes :refer [all-attributes]]))

(defstate automatic-resolvers
  :start
  (vec
    (concat
      (res/generate-resolvers all-attributes)
      (datomic/generate-resolvers all-attributes :production))))
