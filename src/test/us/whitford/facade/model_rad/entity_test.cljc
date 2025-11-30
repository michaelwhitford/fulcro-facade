(ns us.whitford.facade.model-rad.entity-test
  (:require
   [clojure.test :refer [deftest is]]
   [fulcro-spec.core :refer [assertions =>]]
   [us.whitford.facade.model-rad.entity :as entity-rad]
   [com.fulcrologic.rad.attributes :as attr]))

;; Note: defattr macro creates maps with keys in the ::attr/ namespace (com.fulcrologic.rad.attributes)

(deftest entity-attributes-test
  (assertions "entity attributes exist"
    (vector? entity-rad/attributes) => true
    (count entity-rad/attributes) => 3)

  (assertions "entity_id is identity"
    (get entity-rad/entity_id ::attr/qualified-key) => :entity/id
    (get entity-rad/entity_id ::attr/identity?) => true
    (get entity-rad/entity_id ::attr/required?) => true)

  (assertions "entity_name is a string"
    (get entity-rad/entity_name ::attr/qualified-key) => :entity/name
    (get entity-rad/entity_name ::attr/type) => :string)

  (assertions "entity_type is a keyword"
    (get entity-rad/entity_type ::attr/qualified-key) => :entity/type
    (get entity-rad/entity_type ::attr/type) => :keyword))
