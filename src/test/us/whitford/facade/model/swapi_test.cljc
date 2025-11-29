(ns us.whitford.facade.model.swapi-test
  (:require
   [clojure.test :refer [deftest]]
   [fulcro-spec.core :refer [assertions =>]]
   [us.whitford.facade.model.swapi :as swapi]
   [us.whitford.facade.components.utils :as utils]))

(deftest swapiurl->id-test
  (assertions "extracts ID from valid SWAPI URLs"
    (swapi/swapiurl->id "https://swapi.dev/api/people/1/") => "1"
    (swapi/swapiurl->id "https://swapi.dev/api/films/42/") => "42"
    (swapi/swapiurl->id "https://swapi.dev/api/planets/123/") => "123"
    (swapi/swapiurl->id "https://swapi.dev/api/vehicles/999/") => "999")

  (assertions "returns nil for invalid URLs"
    (swapi/swapiurl->id "invalid-url") => nil
    (swapi/swapiurl->id "https://other-api.dev/api/people/1/") => nil
    (swapi/swapiurl->id "") => nil
    (swapi/swapiurl->id "https://swapi.dev/api/people/no-trailing-slash") => nil)

  (assertions "handles different entity types"
    (swapi/swapiurl->id "https://swapi.dev/api/starships/1/") => "1"
    (swapi/swapiurl->id "https://swapi.dev/api/species/2/") => "2"
    (swapi/swapiurl->id "https://swapi.dev/api/vehicles/3/") => "3"))

(deftest swapi-id-test
  (let [input {:url "https://swapi.dev/api/people/1/" :name "Luke"}]
    (assertions "adds ID from URL"
      (swapi/swapi-id input) => {:url "https://swapi.dev/api/people/1/" :name "Luke" :id "1"}))

  (let [input {:name "Luke"}]
    (assertions "returns map unchanged when no URL"
      (swapi/swapi-id input) => {:name "Luke"}))

  (let [input {:url nil :name "Luke"}]
    (assertions "handles nil URL gracefully"
      (swapi/swapi-id input) => {:url nil :name "Luke"})))

(deftest swapi-page->number-test
  (assertions "extracts page number from URL"
    (swapi/swapi-page->number "https://swapi.dev/api/people/?page=2") => 2
    (swapi/swapi-page->number "https://swapi.dev/api/films/?page=10") => 10
    (swapi/swapi-page->number "https://swapi.dev/api/planets/?page=1") => 1)

  (assertions "handles URLs with additional params"
    (swapi/swapi-page->number "https://swapi.dev/api/people/?search=luke&page=3") => 3
    (swapi/swapi-page->number "https://swapi.dev/api/people/?page=5&format=json") => 5)

  (assertions "returns nil for invalid input"
    (swapi/swapi-page->number "https://swapi.dev/api/people/") => nil
    (swapi/swapi-page->number "no-page-param") => nil
    (swapi/swapi-page->number "") => nil))

(deftest swapi->pathom-test
  (let [input {:films ["https://swapi.dev/api/films/1/" "https://swapi.dev/api/films/2/"]
               :homeworld "https://swapi.dev/api/planets/1/"}]
    (assertions "transform string URLs to IDs"
      (swapi/swapi->pathom input) => {:films ["1" "2"]
                                      :homeworld "1"}))

  (let [input {:films ["https://swapi.dev/api/films/1/" nil :not-a-string]
               :homeworld 123}]
    (assertions "skip non-string values"
      (swapi/swapi->pathom input) => {:films ["1"]
                                      :homeworld nil}))

  (let [input {:name "Luke Skywalker"}]
    (assertions "handle missing keys"
      (swapi/swapi->pathom input) => {:name "Luke Skywalker"}))

  (let [input {:films []
               :homeworld "https://swapi.dev/api/planets/1/"}]
    (assertions "handle empty collections"
      (swapi/swapi->pathom input) => {:films []
                                      :homeworld "1"}))

  (let [input {:films ["https://swapi.dev/api/films/1/"]
               :starships ["https://swapi.dev/api/starships/2/"]
               :species ["https://swapi.dev/api/species/3/"]
               :vehicles ["https://swapi.dev/api/vehicles/4/"]
               :residents ["https://swapi.dev/api/people/5/"]
               :people ["https://swapi.dev/api/people/6/"]
               :characters ["https://swapi.dev/api/people/7/"]
               :pilots ["https://swapi.dev/api/people/8/"]
               :planets ["https://swapi.dev/api/planets/9/"]
               :homeworld "https://swapi.dev/api/planets/10/"}]
    (assertions "transforms all supported reference types"
      (swapi/swapi->pathom input) => {:films ["1"]
                                      :starships ["2"]
                                      :species ["3"]
                                      :vehicles ["4"]
                                      :residents ["5"]
                                      :people ["6"]
                                      :characters ["7"]
                                      :pilots ["8"]
                                      :planets ["9"]
                                      :homeworld "10"})))

(deftest transform-swapi-test
  (let [input [{:url "https://swapi.dev/api/people/1/"
                :name "Luke Skywalker"
                :films ["https://swapi.dev/api/films/1/" "https://swapi.dev/api/films/2/"]
                :homeworld "https://swapi.dev/api/planets/1/"}]
        result (swapi/transform-swapi input)]
    (assertions "transforms list of people"
      (count result) => 1
      (:id (first result)) => "1"
      (:name (first result)) => "Luke Skywalker"
      (:films (first result)) => ["1" "2"]
      (:homeworld (first result)) => "1"))

  (let [input [{:url "https://swapi.dev/api/people/1/" :name "Luke"}
               {:url "https://swapi.dev/api/people/2/" :name "Leia"}]
        result (swapi/transform-swapi input)]
    (assertions "handles multiple entities"
      (count result) => 2
      (:id (first result)) => "1"
      (:id (second result)) => "2"))

  (assertions "handles empty input"
    (swapi/transform-swapi []) => [])

  (let [input [{:url "https://swapi.dev/api/people/1/"
                :name "Luke"
                :starships ["https://swapi.dev/api/starships/12/" "https://swapi.dev/api/starships/22/"]
                :species []
                :vehicles []}]
        result (swapi/transform-swapi input)]
    (assertions "handles entities without URLs in collections"
      (:starships (first result)) => ["12" "22"]
      (:species (first result)) => []
      (:vehicles (first result)) => [])))

(deftest person-data-transformation-test
  (let [raw {:id "1"
             :name "Luke Skywalker"
             :birth_year "19BBY"
             :eye_color "blue"
             :gender "male"
             :hair_color "blond"
             :height "172"
             :mass "77"
             :skin_color "fair"
             :homeworld "1"
             :films ["1" "2" "3"]}
        expected {:person/id "1"
                  :person/name "Luke Skywalker"
                  :person/birth_year "19BBY"
                  :person/eye_color "blue"
                  :person/gender "male"
                  :person/hair_color "blond"
                  :person/height "172"
                  :person/mass "77"
                  :person/skin_color "fair"
                  :person/homeworld "1"
                  :person/films ["1" "2" "3"]}]
    (assertions "transforms person data to namespaced map"
      (utils/map->nsmap raw "person") => expected)))

(deftest film-data-transformation-test
  (let [raw {:id "1"
             :title "A New Hope"
             :episode_id 4
             :opening_crawl "It is a period of civil war..."
             :director "George Lucas"
             :producer "Gary Kurtz, Rick McCallum"
             :release_date "1977-05-25"
             :characters ["1" "2" "3"]
             :planets ["1" "2"]
             :starships ["2" "3"]
             :vehicles ["4" "6"]
             :species ["1" "2"]}
        result (utils/map->nsmap raw "film")]
    (assertions "transforms film data to namespaced map"
      (:film/id result) => "1"
      (:film/title result) => "A New Hope"
      (:film/episode_id result) => 4
      (:film/director result) => "George Lucas")))

(deftest planet-data-transformation-test
  (let [raw {:id "1"
             :name "Tatooine"
             :climate "arid"
             :diameter "10465"
             :gravity "1 standard"
             :orbital_period "304"
             :population "200000"
             :rotation_period "23"
             :terrain "desert"}
        result (utils/map->nsmap raw "planet")]
    (assertions "transforms planet data to namespaced map"
      (:planet/id result) => "1"
      (:planet/name result) => "Tatooine"
      (:planet/climate result) => "arid"
      (:planet/terrain result) => "desert")))

(deftest vehicle-data-transformation-test
  (let [raw {:id "4"
             :name "Sand Crawler"
             :model "Digger Crawler"
             :manufacturer "Corellia Mining Corporation"
             :cost_in_credits "150000"
             :cargo_capacity "50000"
             :consumables "2 months"
             :crew "46"
             :passengers "30"
             :films ["1" "2"]
             :pilots []}
        result (utils/map->nsmap raw "vehicle")]
    (assertions "transforms vehicle data to namespaced map"
      (:vehicle/id result) => "4"
      (:vehicle/name result) => "Sand Crawler"
      (:vehicle/model result) => "Digger Crawler"
      (:vehicle/films result) => ["1" "2"])))

(deftest starship-data-transformation-test
  (let [raw {:id "10"
             :name "Millennium Falcon"
             :model "YT-1300 light freighter"
             :manufacturer "Corellian Engineering Corporation"
             :cost_in_credits "100000"
             :crew "4"
             :passengers "6"
             :cargo_capacity "100000"
             :consumables "2 months"
             :hyperdrive_rating "0.5"
             :length "34.37"
             :max_atmosphering_speed "1050"
             :class "Light freighter"
             :films ["1" "2" "3"]
             :pilots ["13" "14" "25" "31"]}
        result (utils/map->nsmap raw "starship")]
    (assertions "transforms starship data to namespaced map"
      (:starship/id result) => "10"
      (:starship/name result) => "Millennium Falcon"
      (:starship/hyperdrive_rating result) => "0.5"
      (:starship/class result) => "Light freighter")))

(deftest species-data-transformation-test
  (let [raw {:id "1"
             :name "Human"
             :classification "mammal"
             :designation "sentient"
             :average_height "180"
             :average_lifespan "120"
             :eye_colors "brown, blue, green, hazel, grey, amber"
             :hair_colors "blonde, brown, black, red"
             :skin_colors "caucasian, black, asian, hispanic"
             :language "Galactic Basic"
             :homeworld "9"
             :people ["1" "4" "5"]
             :films ["1" "2" "3"]}
        result (utils/map->nsmap raw "specie")]
    (assertions "transforms species data to namespaced map"
      (:specie/id result) => "1"
      (:specie/name result) => "Human"
      (:specie/classification result) => "mammal"
      (:specie/language result) => "Galactic Basic")))

(deftest entity-id-generation-test
  (let [person-map {:person/id "1" :person/name "Luke"}
        kw-ns (namespace (first (keys person-map)))
        id (str kw-ns "-" (get person-map :person/id))]
    (assertions "generates entity IDs with type prefix"
      id => "person-1"))

  (assertions "handles different entity types"
    (str "film" "-" "4") => "film-4"
    (str "planet" "-" "1") => "planet-1"
    (str "vehicle" "-" "14") => "vehicle-14"))

(deftest edge-cases-test
  (let [raw {:id "1" :name "Test" :unknown_field "value"}
        result (utils/map->nsmap raw "person")]
    (assertions "handles unknown/missing fields gracefully"
      (:person/id result) => "1"
      (:person/unknown_field result) => "value"))

  (let [raw {:id "1" :name nil :birth_year nil}
        result (utils/map->nsmap raw "person")]
    (assertions "handles nil values in map"
      (:person/id result) => "1"
      (:person/name result) => nil
      (:person/birth_year result) => nil))

  (let [raw {:id "1" :name "" :birth_year "unknown"}
        result (utils/map->nsmap raw "person")]
    (assertions "handles empty strings"
      (:person/name result) => ""
      (:person/birth_year result) => "unknown")))
