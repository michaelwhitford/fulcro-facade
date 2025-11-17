(ns us.whitford.facade.model-rad.swapi-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [us.whitford.facade.model-rad.swapi :as swapi-rad]
   [com.fulcrologic.rad.attributes :as attr]))

;; Note: defattr macro creates maps with keys in the ::attr/ namespace (com.fulcrologic.rad.attributes)

(deftest test-person-attributes
  (testing "person-attributes contains all required attributes"
    (is (vector? swapi-rad/person-attributes))
    (is (pos? (count swapi-rad/person-attributes))))

  (testing "person_id is an identity attribute"
    (is (map? swapi-rad/person_id))
    (is (= :person/id (get swapi-rad/person_id ::attr/qualified-key)))
    (is (true? (get swapi-rad/person_id ::attr/identity?)))
    (is (= :string (get swapi-rad/person_id ::attr/type))))

  (testing "person_name is a string attribute"
    (is (map? swapi-rad/person_name))
    (is (= :person/name (get swapi-rad/person_name ::attr/qualified-key)))
    (is (= :string (get swapi-rad/person_name ::attr/type)))))

(deftest test-film-attributes
  (testing "film-attributes is a vector"
    (is (vector? swapi-rad/film-attributes))
    (is (pos? (count swapi-rad/film-attributes))))

  (testing "film_id is an identity attribute"
    (is (map? swapi-rad/film_id))
    (is (= :film/id (get swapi-rad/film_id ::attr/qualified-key)))
    (is (true? (get swapi-rad/film_id ::attr/identity?))))

  (testing "film_title is a string attribute"
    (is (= :string (get swapi-rad/film_title ::attr/type)))
    (is (contains? (get swapi-rad/film_title ::attr/identities) :film/id)))

  (testing "film_episode_id is an integer attribute"
    (is (= :int (get swapi-rad/film_episode_id ::attr/type))))

  (testing "film_characters is a ref with many cardinality"
    (is (= :ref (get swapi-rad/film_characters ::attr/type)))
    (is (= :many (get swapi-rad/film_characters ::attr/cardinality)))))

(deftest test-planet-attributes
  (testing "planet-attributes is a vector"
    (is (vector? swapi-rad/planet-attributes))
    (is (= 9 (count swapi-rad/planet-attributes))))

  (testing "planet_id is an identity attribute"
    (is (= :planet/id (get swapi-rad/planet_id ::attr/qualified-key)))
    (is (true? (get swapi-rad/planet_id ::attr/identity?))))

  (testing "all planet string attributes have correct identities"
    (doseq [a [swapi-rad/planet_name swapi-rad/planet_climate
               swapi-rad/planet_gravity swapi-rad/planet_terrain]]
      (is (contains? (get a ::attr/identities) :planet/id))
      (is (= :string (get a ::attr/type))))))

(deftest test-species-attributes
  (testing "species-attributes contains all attributes"
    (is (vector? swapi-rad/species-attributes))
    (is (= 13 (count swapi-rad/species-attributes))))

  (testing "species_id is an identity (uses :specie namespace)"
    (is (= :specie/id (get swapi-rad/species_id ::attr/qualified-key)))
    (is (true? (get swapi-rad/species_id ::attr/identity?))))

  (testing "species_homeworld is a ref with one cardinality"
    (is (= :ref (get swapi-rad/species_homeworld ::attr/type)))
    (is (= :planet/id (get swapi-rad/species_homeworld ::attr/target)))
    (is (= :one (get swapi-rad/species_homeworld ::attr/cardinality))))

  (testing "species_people is a ref with many cardinality"
    (is (= :ref (get swapi-rad/species_people ::attr/type)))
    (is (= :person/id (get swapi-rad/species_people ::attr/target)))
    (is (= :many (get swapi-rad/species_people ::attr/cardinality)))))

(deftest test-vehicle-attributes
  (testing "vehicle-attributes is complete"
    (is (vector? swapi-rad/vehicle-attributes))
    (is (= 11 (count swapi-rad/vehicle-attributes))))

  (testing "vehicle_id is an identity"
    (is (= :vehicle/id (get swapi-rad/vehicle_id ::attr/qualified-key)))
    (is (true? (get swapi-rad/vehicle_id ::attr/identity?))))

  (testing "vehicle_films references films"
    (is (= :ref (get swapi-rad/vehicle_films ::attr/type)))
    (is (= :film/id (get swapi-rad/vehicle_films ::attr/target)))
    (is (= :many (get swapi-rad/vehicle_films ::attr/cardinality))))

  (testing "vehicle_pilots references people"
    (is (= :ref (get swapi-rad/vehicle_pilots ::attr/type)))
    (is (= :person/id (get swapi-rad/vehicle_pilots ::attr/target)))))

(deftest test-starship-attributes
  (testing "starship-attributes is complete"
    (is (vector? swapi-rad/starship-attributes))
    (is (= 15 (count swapi-rad/starship-attributes))))

  (testing "starship_id is an identity"
    (is (= :starship/id (get swapi-rad/starship_id ::attr/qualified-key)))
    (is (true? (get swapi-rad/starship_id ::attr/identity?))))

  (testing "starship has numeric fields as strings"
    (doseq [a [swapi-rad/starship_cargo_capacity swapi-rad/starship_cost_in_credits
               swapi-rad/starship_hyperdrive_rating swapi-rad/starship_length]]
      (is (= :string (get a ::attr/type)))))

  (testing "starship_class is a string"
    (is (= :starship/class (get swapi-rad/starship_class ::attr/qualified-key)))
    (is (= :string (get swapi-rad/starship_class ::attr/type)))))

(deftest test-entity-attributes
  (testing "entity-attributes exist"
    (is (vector? swapi-rad/entity-attributes))
    (is (= 3 (count swapi-rad/entity-attributes))))

  (testing "entity_id is identity"
    (is (= :entity/id (get swapi-rad/entity_id ::attr/qualified-key)))
    (is (true? (get swapi-rad/entity_id ::attr/identity?)))
    (is (true? (get swapi-rad/entity_id ::attr/required?))))

  (testing "entity_type is a keyword"
    (is (= :entity/type (get swapi-rad/entity_type ::attr/qualified-key)))
    (is (= :keyword (get swapi-rad/entity_type ::attr/type)))))

(deftest test-all-attributes
  (testing "attributes vector contains all entity types"
    (is (vector? swapi-rad/attributes))
    (is (pos? (count swapi-rad/attributes))))

  (testing "attributes includes all sub-vectors"
    (let [total-count (count swapi-rad/attributes)
          expected-count (+ (count swapi-rad/entity-attributes)
                            (count swapi-rad/person-attributes)
                            (count swapi-rad/planet-attributes)
                            (count swapi-rad/species-attributes)
                            (count swapi-rad/film-attributes)
                            (count swapi-rad/vehicle-attributes)
                            (count swapi-rad/starship-attributes))]
      (is (= expected-count total-count))))

  (testing "all attributes are maps"
    (doseq [a swapi-rad/attributes]
      (is (map? a) (str "Expected map but got: " (type a)))))

  (testing "all attributes have qualified keys"
    (doseq [a swapi-rad/attributes]
      (is (contains? a ::attr/qualified-key))))

  (testing "all attributes have types"
    (doseq [a swapi-rad/attributes]
      (is (contains? a ::attr/type)))))

(deftest test-attribute-consistency
  (testing "all identity attributes are required or correctly configured"
    (let [identity-attrs (filter #(get % ::attr/identity?) swapi-rad/attributes)]
      (is (pos? (count identity-attrs)))
      (doseq [a identity-attrs]
        (is (keyword? (get a ::attr/qualified-key))))))

  (testing "all ref attributes have targets"
    (let [ref-attrs (filter #(= :ref (get % ::attr/type)) swapi-rad/attributes)]
      (doseq [a ref-attrs]
        (is (keyword? (get a ::attr/target))
            (str "Missing target for " (get a ::attr/qualified-key)))
        (is (contains? #{:one :many} (get a ::attr/cardinality))
            (str "Missing cardinality for " (get a ::attr/qualified-key)))))))

(deftest test-namespace-consistency
  (testing "person attributes use :person namespace"
    (doseq [a [swapi-rad/person_id swapi-rad/person_name]]
      (is (= "person" (namespace (get a ::attr/qualified-key))))))

  (testing "film attributes use :film namespace"
    (doseq [a [swapi-rad/film_id swapi-rad/film_title swapi-rad/film_director]]
      (is (= "film" (namespace (get a ::attr/qualified-key))))))

  (testing "planet attributes use :planet namespace"
    (doseq [a [swapi-rad/planet_id swapi-rad/planet_name swapi-rad/planet_climate]]
      (is (= "planet" (namespace (get a ::attr/qualified-key))))))

  (testing "species attributes use :specie namespace (singular)"
    (doseq [a [swapi-rad/species_id swapi-rad/species_name swapi-rad/species_language]]
      (is (= "specie" (namespace (get a ::attr/qualified-key))))))

  (testing "vehicle attributes use :vehicle namespace"
    (doseq [a [swapi-rad/vehicle_id swapi-rad/vehicle_name swapi-rad/vehicle_model]]
      (is (= "vehicle" (namespace (get a ::attr/qualified-key))))))

  (testing "starship attributes use :starship namespace"
    (doseq [a [swapi-rad/starship_id swapi-rad/starship_name swapi-rad/starship_class]]
      (is (= "starship" (namespace (get a ::attr/qualified-key)))))))
