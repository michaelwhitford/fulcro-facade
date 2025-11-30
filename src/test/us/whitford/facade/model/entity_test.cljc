(ns us.whitford.facade.model.entity-test
  "Tests for entity transformation functions.
   These are pure function tests that do not require a running app."
  (:require
   [clojure.test :refer [deftest is]]
   [fulcro-spec.core :refer [assertions =>]]
   [us.whitford.facade.model.entity :as entity]))

(deftest swapi-entity->unified-test
  (let [person {:person/id "1"
                :person/name "Luke Skywalker"
                :person/birth_year "19BBY"}
        result (entity/swapi-entity->unified person)]
    (assertions "transforms person entity correctly"
      (:entity/id result) => "person-1"
      (:entity/name result) => "Luke Skywalker"
      (:entity/type result) => :person))

  (let [film {:film/id "4"
              :film/title "A New Hope"
              :film/director "George Lucas"}
        result (entity/swapi-entity->unified film)]
    (assertions "transforms film entity using :title instead of :name"
      (:entity/id result) => "film-4"
      (:entity/name result) => "A New Hope"
      (:entity/type result) => :film))

  (let [planet {:planet/id "1"
                :planet/name "Tatooine"
                :planet/climate "arid"}
        result (entity/swapi-entity->unified planet)]
    (assertions "transforms planet entity correctly"
      (:entity/id result) => "planet-1"
      (:entity/name result) => "Tatooine"
      (:entity/type result) => :planet))

  (let [starship {:starship/id "10"
                  :starship/name "Millennium Falcon"
                  :starship/model "YT-1300"}
        result (entity/swapi-entity->unified starship)]
    (assertions "transforms starship entity correctly"
      (:entity/id result) => "starship-10"
      (:entity/name result) => "Millennium Falcon"
      (:entity/type result) => :starship))

  (assertions "returns nil for empty map"
    (entity/swapi-entity->unified {}) => nil)

  (assertions "returns nil for map without namespaced keys"
    (entity/swapi-entity->unified {:id "1" :name "Test"}) => nil)

  (let [missing-id {:person/name "No ID Person"}]
    (assertions "returns nil when id is missing"
      (entity/swapi-entity->unified missing-id) => nil)))

(deftest hpapi-character->unified-test
  (let [character {:character/id "9e3f7ce4-b9a7-4244-b709-dae5c1f1d4a8"
                   :character/name "Harry Potter"
                   :character/house "Gryffindor"}
        result (entity/hpapi-character->unified character)]
    (assertions "transforms HP character correctly"
      (:entity/id result) => "character-9e3f7ce4-b9a7-4244-b709-dae5c1f1d4a8"
      (:entity/name result) => "Harry Potter"
      (:entity/type result) => :character))

  (assertions "returns nil for character without id"
    (entity/hpapi-character->unified {:character/name "No ID"}) => nil)

  (assertions "returns nil for nil input"
    (entity/hpapi-character->unified nil) => nil))

(deftest hpapi-spell->unified-test
  (let [spell {:spell/id "723dd9c9-ee62-495b-9071-cddd16087b86"
               :spell/name "Levicorpus"
               :spell/description "Levitates the target"}
        result (entity/hpapi-spell->unified spell)]
    (assertions "transforms HP spell correctly"
      (:entity/id result) => "spell-723dd9c9-ee62-495b-9071-cddd16087b86"
      (:entity/name result) => "Levicorpus"
      (:entity/type result) => :spell))

  (assertions "returns nil for spell without id"
    (entity/hpapi-spell->unified {:spell/name "No ID"}) => nil)

  (assertions "returns nil for nil input"
    (entity/hpapi-spell->unified nil) => nil))

(deftest entity-id-format-test
  (assertions "SWAPI entity IDs use type-numericid format"
    (:entity/id (entity/swapi-entity->unified {:person/id "1" :person/name "Test"})) => "person-1"
    (:entity/id (entity/swapi-entity->unified {:film/id "4" :film/title "Test"})) => "film-4"
    (:entity/id (entity/swapi-entity->unified {:vehicle/id "14" :vehicle/name "Test"})) => "vehicle-14")

  (assertions "HP entity IDs use type-uuid format"
    (:entity/id (entity/hpapi-character->unified {:character/id "abc-123" :character/name "Test"})) => "character-abc-123"
    (:entity/id (entity/hpapi-spell->unified {:spell/id "def-456" :spell/name "Test"})) => "spell-def-456"))

(deftest entity-type-inference-test
  (assertions "entity type is inferred from namespace"
    (:entity/type (entity/swapi-entity->unified {:person/id "1" :person/name "Test"})) => :person
    (:entity/type (entity/swapi-entity->unified {:film/id "1" :film/title "Test"})) => :film
    (:entity/type (entity/swapi-entity->unified {:planet/id "1" :planet/name "Test"})) => :planet
    (:entity/type (entity/swapi-entity->unified {:specie/id "1" :specie/name "Test"})) => :specie
    (:entity/type (entity/swapi-entity->unified {:vehicle/id "1" :vehicle/name "Test"})) => :vehicle
    (:entity/type (entity/swapi-entity->unified {:starship/id "1" :starship/name "Test"})) => :starship)

  (assertions "HP types are explicit"
    (:entity/type (entity/hpapi-character->unified {:character/id "1" :character/name "Test"})) => :character
    (:entity/type (entity/hpapi-spell->unified {:spell/id "1" :spell/name "Test"})) => :spell))
