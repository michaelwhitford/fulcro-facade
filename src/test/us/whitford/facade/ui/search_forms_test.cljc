(ns us.whitford.facade.ui.search-forms-test
  "Tests for search form helper functions.
   These are pure function tests that do not require a running app."
  (:require
   [clojure.test :refer [deftest is]]
   [fulcro-spec.core :refer [assertions =>]]
   [us.whitford.facade.ui.search-forms :as sf]))

(deftest parse-entity-id-test
  (assertions "parses SWAPI numeric IDs"
    (sf/parse-entity-id "person-1") => "1"
    (sf/parse-entity-id "film-4") => "4"
    (sf/parse-entity-id "starship-10") => "10"
    (sf/parse-entity-id "vehicle-14") => "14"
    (sf/parse-entity-id "planet-1") => "1"
    (sf/parse-entity-id "specie-3") => "3")

  (assertions "parses HP UUID IDs"
    (sf/parse-entity-id "character-9e3f7ce4-b9a7-4244-b709-dae5c1f1d4a8") => "9e3f7ce4-b9a7-4244-b709-dae5c1f1d4a8"
    (sf/parse-entity-id "spell-723dd9c9-ee62-495b-9071-cddd16087b86") => "723dd9c9-ee62-495b-9071-cddd16087b86")

  (assertions "handles edge cases"
    (sf/parse-entity-id nil) => nil
    (sf/parse-entity-id "") => nil
    (sf/parse-entity-id "invalid") => nil
    (sf/parse-entity-id "no-dash-here") => "dash-here")

  (assertions "handles malformed IDs"
    (sf/parse-entity-id "-") => nil
    (sf/parse-entity-id "person-") => nil
    (sf/parse-entity-id "-1") => nil
    (sf/parse-entity-id "a-b-c-d") => "b-c-d"))

(deftest entity-type-icon-test
  (assertions "returns correct icons for SWAPI types"
    (sf/entity-type-icon :person) => "user"
    (sf/entity-type-icon :film) => "film"
    (sf/entity-type-icon :vehicle) => "car"
    (sf/entity-type-icon :starship) => "space shuttle"
    (sf/entity-type-icon :specie) => "hand spock"
    (sf/entity-type-icon :planet) => "globe")

  (assertions "returns correct icons for HP types"
    (sf/entity-type-icon :character) => "magic"
    (sf/entity-type-icon :spell) => "bolt")

  (assertions "returns question mark for unknown types"
    (sf/entity-type-icon :unknown) => "question"
    (sf/entity-type-icon nil) => "question"
    (sf/entity-type-icon :something-else) => "question"))

(deftest entity-type->form-test
  (assertions "contains all 8 entity types"
    (count sf/entity-type->form) => 8)

  (assertions "maps all SWAPI types"
    (contains? sf/entity-type->form :person) => true
    (contains? sf/entity-type->form :film) => true
    (contains? sf/entity-type->form :vehicle) => true
    (contains? sf/entity-type->form :starship) => true
    (contains? sf/entity-type->form :specie) => true
    (contains? sf/entity-type->form :planet) => true)

  (assertions "maps all HP types"
    (contains? sf/entity-type->form :character) => true
    (contains? sf/entity-type->form :spell) => true))
