(ns us.whitford.facade.model-rad.swapi-test
  (:require
   [clojure.test :refer [deftest is]]
   [fulcro-spec.core :refer [assertions =>]]
   [us.whitford.facade.model-rad.swapi :as swapi-rad]
   [com.fulcrologic.rad.attributes :as attr]))

;; Note: defattr macro creates maps with keys in the ::attr/ namespace (com.fulcrologic.rad.attributes)

(deftest person-attributes-test
  (assertions "person-attributes contains all required attributes"
    (vector? swapi-rad/person-attributes) => true
    (pos? (count swapi-rad/person-attributes)) => true)

  (assertions "person_id is an identity attribute"
    (map? swapi-rad/person_id) => true
    (get swapi-rad/person_id ::attr/qualified-key) => :person/id
    (get swapi-rad/person_id ::attr/identity?) => true
    (get swapi-rad/person_id ::attr/type) => :string)

  (assertions "person_name is a string attribute"
    (map? swapi-rad/person_name) => true
    (get swapi-rad/person_name ::attr/qualified-key) => :person/name
    (get swapi-rad/person_name ::attr/type) => :string))

(deftest film-attributes-test
  (assertions "film-attributes is a vector"
    (vector? swapi-rad/film-attributes) => true
    (pos? (count swapi-rad/film-attributes)) => true)

  (assertions "film_id is an identity attribute"
    (map? swapi-rad/film_id) => true
    (get swapi-rad/film_id ::attr/qualified-key) => :film/id
    (get swapi-rad/film_id ::attr/identity?) => true)

  (assertions "film_title is a string attribute"
    (get swapi-rad/film_title ::attr/type) => :string
    (contains? (get swapi-rad/film_title ::attr/identities) :film/id) => true)

  (assertions "film_episode_id is an integer attribute"
    (get swapi-rad/film_episode_id ::attr/type) => :int)

  (assertions "film_characters is a ref with many cardinality"
    (get swapi-rad/film_characters ::attr/type) => :ref
    (get swapi-rad/film_characters ::attr/cardinality) => :many))

(deftest planet-attributes-test
  (assertions "planet-attributes is a vector"
    (vector? swapi-rad/planet-attributes) => true
    (count swapi-rad/planet-attributes) => 9)

  (assertions "planet_id is an identity attribute"
    (get swapi-rad/planet_id ::attr/qualified-key) => :planet/id
    (get swapi-rad/planet_id ::attr/identity?) => true)

  ;; Use is for doseq since => doesn't work inside loops
  (doseq [a [swapi-rad/planet_name swapi-rad/planet_climate
             swapi-rad/planet_gravity swapi-rad/planet_terrain]]
    (is (contains? (get a ::attr/identities) :planet/id) "all planet string attributes have correct identities")
    (is (= :string (get a ::attr/type)) "all planet string attributes have correct identities")))

(deftest species-attributes-test
  (assertions "species-attributes contains all attributes"
    (vector? swapi-rad/species-attributes) => true
    (count swapi-rad/species-attributes) => 13)

  (assertions "species_id is an identity (uses :specie namespace)"
    (get swapi-rad/species_id ::attr/qualified-key) => :specie/id
    (get swapi-rad/species_id ::attr/identity?) => true)

  (assertions "species_homeworld is a ref with one cardinality"
    (get swapi-rad/species_homeworld ::attr/type) => :ref
    (get swapi-rad/species_homeworld ::attr/target) => :planet/id
    (get swapi-rad/species_homeworld ::attr/cardinality) => :one)

  (assertions "species_people is a ref with many cardinality"
    (get swapi-rad/species_people ::attr/type) => :ref
    (get swapi-rad/species_people ::attr/target) => :person/id
    (get swapi-rad/species_people ::attr/cardinality) => :many))

(deftest vehicle-attributes-test
  (assertions "vehicle-attributes is complete"
    (vector? swapi-rad/vehicle-attributes) => true
    (count swapi-rad/vehicle-attributes) => 11)

  (assertions "vehicle_id is an identity"
    (get swapi-rad/vehicle_id ::attr/qualified-key) => :vehicle/id
    (get swapi-rad/vehicle_id ::attr/identity?) => true)

  (assertions "vehicle_films references films"
    (get swapi-rad/vehicle_films ::attr/type) => :ref
    (get swapi-rad/vehicle_films ::attr/target) => :film/id
    (get swapi-rad/vehicle_films ::attr/cardinality) => :many)

  (assertions "vehicle_pilots references people"
    (get swapi-rad/vehicle_pilots ::attr/type) => :ref
    (get swapi-rad/vehicle_pilots ::attr/target) => :person/id))

(deftest starship-attributes-test
  (assertions "starship-attributes is complete"
    (vector? swapi-rad/starship-attributes) => true
    (count swapi-rad/starship-attributes) => 15)

  (assertions "starship_id is an identity"
    (get swapi-rad/starship_id ::attr/qualified-key) => :starship/id
    (get swapi-rad/starship_id ::attr/identity?) => true)

  ;; Use is for doseq since => doesn't work inside loops
  (doseq [a [swapi-rad/starship_cargo_capacity swapi-rad/starship_cost_in_credits
             swapi-rad/starship_hyperdrive_rating swapi-rad/starship_length]]
    (is (= :string (get a ::attr/type)) "starship has numeric fields as strings"))

  (assertions "starship_class is a string"
    (get swapi-rad/starship_class ::attr/qualified-key) => :starship/class
    (get swapi-rad/starship_class ::attr/type) => :string))

(deftest entity-attributes-test
  (assertions "entity-attributes exist"
    (vector? swapi-rad/entity-attributes) => true
    (count swapi-rad/entity-attributes) => 3)

  (assertions "entity_id is identity"
    (get swapi-rad/entity_id ::attr/qualified-key) => :entity/id
    (get swapi-rad/entity_id ::attr/identity?) => true
    (get swapi-rad/entity_id ::attr/required?) => true)

  (assertions "entity_type is a keyword"
    (get swapi-rad/entity_type ::attr/qualified-key) => :entity/type
    (get swapi-rad/entity_type ::attr/type) => :keyword))

(deftest all-attributes-test
  (assertions "attributes vector contains all entity types"
    (vector? swapi-rad/attributes) => true
    (pos? (count swapi-rad/attributes)) => true)

  (let [total-count (count swapi-rad/attributes)
        expected-count (+ (count swapi-rad/entity-attributes)
                          (count swapi-rad/person-attributes)
                          (count swapi-rad/planet-attributes)
                          (count swapi-rad/species-attributes)
                          (count swapi-rad/film-attributes)
                          (count swapi-rad/vehicle-attributes)
                          (count swapi-rad/starship-attributes))]
    (assertions "attributes includes all sub-vectors"
      total-count => expected-count))

  ;; Use is for doseq since => doesn't work inside loops
  (doseq [a swapi-rad/attributes]
    (is (map? a) "all attributes are maps"))

  (doseq [a swapi-rad/attributes]
    (is (contains? a ::attr/qualified-key) "all attributes have qualified keys"))

  (doseq [a swapi-rad/attributes]
    (is (contains? a ::attr/type) "all attributes have types")))

(deftest attribute-consistency-test
  (let [identity-attrs (filter #(get % ::attr/identity?) swapi-rad/attributes)]
    (assertions "all identity attributes are required or correctly configured"
      (pos? (count identity-attrs)) => true)
    (doseq [a identity-attrs]
      (is (keyword? (get a ::attr/qualified-key)) "all identity attributes are required or correctly configured")))

  (let [ref-attrs (filter #(= :ref (get % ::attr/type)) swapi-rad/attributes)]
    (doseq [a ref-attrs]
      (is (keyword? (get a ::attr/target)) "all ref attributes have targets")
      (is (contains? #{:one :many} (get a ::attr/cardinality)) "all ref attributes have targets"))))

(deftest namespace-consistency-test
  (doseq [a [swapi-rad/person_id swapi-rad/person_name]]
    (is (= "person" (namespace (get a ::attr/qualified-key))) "person attributes use :person namespace"))

  (doseq [a [swapi-rad/film_id swapi-rad/film_title swapi-rad/film_director]]
    (is (= "film" (namespace (get a ::attr/qualified-key))) "film attributes use :film namespace"))

  (doseq [a [swapi-rad/planet_id swapi-rad/planet_name swapi-rad/planet_climate]]
    (is (= "planet" (namespace (get a ::attr/qualified-key))) "planet attributes use :planet namespace"))

  (doseq [a [swapi-rad/species_id swapi-rad/species_name swapi-rad/species_language]]
    (is (= "specie" (namespace (get a ::attr/qualified-key))) "species attributes use :specie namespace (singular)"))

  (doseq [a [swapi-rad/vehicle_id swapi-rad/vehicle_name swapi-rad/vehicle_model]]
    (is (= "vehicle" (namespace (get a ::attr/qualified-key))) "vehicle attributes use :vehicle namespace"))

  (doseq [a [swapi-rad/starship_id swapi-rad/starship_name swapi-rad/starship_class]]
    (is (= "starship" (namespace (get a ::attr/qualified-key))) "starship attributes use :starship namespace")))
