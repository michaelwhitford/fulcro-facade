(ns us.whitford.facade.model-rad.entity
  "RAD attributes for universal search entities.
   These represent a unified view across all backend APIs (SWAPI, Harry Potter, etc.)"
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]))

;; Universal entity attributes for :swapi/all-entities (and future :entity/all-entities)
;; These provide a common interface for search results across all APIs

(defattr entity_id :entity/id :string
  {ao/identity? true
   ao/required? true})

(defattr entity_name :entity/name :string
  {ao/identities #{:entity/id}})

(defattr entity_type :entity/type :keyword
  {ao/identities #{:entity/id}})

(def attributes
  [entity_id entity_name entity_type])
