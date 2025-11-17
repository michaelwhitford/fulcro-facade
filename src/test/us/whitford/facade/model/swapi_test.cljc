(ns us.whitford.facade.model.swapi-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [us.whitford.facade.model.swapi :as swapi]
   [us.whitford.facade.components.utils :as utils]))

(deftest test-swapiurl->id
  (testing "extracts ID from valid SWAPI URLs"
    (is (= "1" (swapi/swapiurl->id "https://swapi.dev/api/people/1/")))
    (is (= "42" (swapi/swapiurl->id "https://swapi.dev/api/films/42/")))
    (is (= "123" (swapi/swapiurl->id "https://swapi.dev/api/planets/123/")))
    (is (= "999" (swapi/swapiurl->id "https://swapi.dev/api/vehicles/999/"))))

  (testing "returns nil for invalid URLs"
    (is (nil? (swapi/swapiurl->id "invalid-url")))
    (is (nil? (swapi/swapiurl->id "https://other-api.dev/api/people/1/")))
    (is (nil? (swapi/swapiurl->id "")))
    (is (nil? (swapi/swapiurl->id "https://swapi.dev/api/people/no-trailing-slash"))))

  (testing "handles different entity types"
    (is (= "1" (swapi/swapiurl->id "https://swapi.dev/api/starships/1/")))
    (is (= "2" (swapi/swapiurl->id "https://swapi.dev/api/species/2/")))
    (is (= "3" (swapi/swapiurl->id "https://swapi.dev/api/vehicles/3/")))))

(deftest test-swapi-id
  (testing "adds ID from URL"
    (let [input {:url "https://swapi.dev/api/people/1/" :name "Luke"}]
      (is (= {:url "https://swapi.dev/api/people/1/" :name "Luke" :id "1"}
             (swapi/swapi-id input)))))

  (testing "returns map unchanged when no URL"
    (let [input {:name "Luke"}]
      (is (= {:name "Luke"} (swapi/swapi-id input)))))

  (testing "handles nil URL gracefully"
    (let [input {:url nil :name "Luke"}]
      (is (= {:url nil :name "Luke"} (swapi/swapi-id input))))))

(deftest test-swapi-page->number
  (testing "extracts page number from URL"
    (is (= 2 (swapi/swapi-page->number "https://swapi.dev/api/people/?page=2")))
    (is (= 10 (swapi/swapi-page->number "https://swapi.dev/api/films/?page=10")))
    (is (= 1 (swapi/swapi-page->number "https://swapi.dev/api/planets/?page=1"))))

  (testing "handles URLs with additional params"
    (is (= 3 (swapi/swapi-page->number "https://swapi.dev/api/people/?search=luke&page=3")))
    (is (= 5 (swapi/swapi-page->number "https://swapi.dev/api/people/?page=5&format=json"))))

  (testing "returns nil for invalid input"
    (is (nil? (swapi/swapi-page->number "https://swapi.dev/api/people/")))
    (is (nil? (swapi/swapi-page->number "no-page-param")))
    (is (nil? (swapi/swapi-page->number "")))))

(deftest test-swapi->pathom
  (testing "transform string URLs to IDs"
    (let [input {:films ["https://swapi.dev/api/films/1/" "https://swapi.dev/api/films/2/"]
                 :homeworld "https://swapi.dev/api/planets/1/"}]
      (is (= {:films ["1" "2"]
              :homeworld "1"}
             (swapi/swapi->pathom input)))))

  (testing "skip non-string values"
    (let [input {:films ["https://swapi.dev/api/films/1/" nil :not-a-string]
                 :homeworld 123}]
      (is (= {:films ["1"]
              :homeworld nil}
             (swapi/swapi->pathom input)))))

  (testing "handle missing keys"
    (let [input {:name "Luke Skywalker"}]
      (is (= {:name "Luke Skywalker"}
             (swapi/swapi->pathom input)))))

  (testing "handle empty collections"
    (let [input {:films []
                 :homeworld "https://swapi.dev/api/planets/1/"}]
      (is (= {:films []
              :homeworld "1"}
             (swapi/swapi->pathom input)))))

  (testing "transforms all supported reference types"
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
      (is (= {:films ["1"]
              :starships ["2"]
              :species ["3"]
              :vehicles ["4"]
              :residents ["5"]
              :people ["6"]
              :characters ["7"]
              :pilots ["8"]
              :planets ["9"]
              :homeworld "10"}
             (swapi/swapi->pathom input))))))

(deftest test-transform-swapi
  (testing "transforms list of people"
    (let [input [{:url "https://swapi.dev/api/people/1/"
                  :name "Luke Skywalker"
                  :films ["https://swapi.dev/api/films/1/" "https://swapi.dev/api/films/2/"]
                  :homeworld "https://swapi.dev/api/planets/1/"}]
          result (swapi/transform-swapi input)]
      (is (= 1 (count result)))
      (is (= "1" (:id (first result))))
      (is (= "Luke Skywalker" (:name (first result))))
      (is (= ["1" "2"] (:films (first result))))
      (is (= "1" (:homeworld (first result))))))

  (testing "handles multiple entities"
    (let [input [{:url "https://swapi.dev/api/people/1/" :name "Luke"}
                 {:url "https://swapi.dev/api/people/2/" :name "Leia"}]
          result (swapi/transform-swapi input)]
      (is (= 2 (count result)))
      (is (= "1" (:id (first result))))
      (is (= "2" (:id (second result))))))

  (testing "handles empty input"
    (is (= [] (swapi/transform-swapi []))))

  (testing "handles entities without URLs in collections"
    (let [input [{:url "https://swapi.dev/api/people/1/"
                  :name "Luke"
                  :starships ["https://swapi.dev/api/starships/12/" "https://swapi.dev/api/starships/22/"]
                  :species []
                  :vehicles []}]
          result (swapi/transform-swapi input)]
      (is (= ["12" "22"] (:starships (first result))))
      (is (= [] (:species (first result))))
      (is (= [] (:vehicles (first result)))))))

(deftest test-person-data-transformation
  (testing "transforms person data to namespaced map"
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
      (is (= expected (utils/map->nsmap raw "person"))))))

(deftest test-film-data-transformation
  (testing "transforms film data to namespaced map"
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
      (is (= "1" (:film/id result)))
      (is (= "A New Hope" (:film/title result)))
      (is (= 4 (:film/episode_id result)))
      (is (= "George Lucas" (:film/director result))))))

(deftest test-planet-data-transformation
  (testing "transforms planet data to namespaced map"
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
      (is (= "1" (:planet/id result)))
      (is (= "Tatooine" (:planet/name result)))
      (is (= "arid" (:planet/climate result)))
      (is (= "desert" (:planet/terrain result))))))

(deftest test-vehicle-data-transformation
  (testing "transforms vehicle data to namespaced map"
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
      (is (= "4" (:vehicle/id result)))
      (is (= "Sand Crawler" (:vehicle/name result)))
      (is (= "Digger Crawler" (:vehicle/model result)))
      (is (= ["1" "2"] (:vehicle/films result))))))

(deftest test-starship-data-transformation
  (testing "transforms starship data to namespaced map"
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
      (is (= "10" (:starship/id result)))
      (is (= "Millennium Falcon" (:starship/name result)))
      (is (= "0.5" (:starship/hyperdrive_rating result)))
      (is (= "Light freighter" (:starship/class result))))))

(deftest test-species-data-transformation
  (testing "transforms species data to namespaced map"
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
      (is (= "1" (:specie/id result)))
      (is (= "Human" (:specie/name result)))
      (is (= "mammal" (:specie/classification result)))
      (is (= "Galactic Basic" (:specie/language result))))))

(deftest test-entity-id-generation
  (testing "generates entity IDs with type prefix"
    (let [person-map {:person/id "1" :person/name "Luke"}
          kw-ns (namespace (first (keys person-map)))
          id (str kw-ns "-" (get person-map :person/id))]
      (is (= "person-1" id))))

  (testing "handles different entity types"
    (is (= "film-4" (str "film" "-" "4")))
    (is (= "planet-1" (str "planet" "-" "1")))
    (is (= "vehicle-14" (str "vehicle" "-" "14")))))

(deftest test-edge-cases
  (testing "handles unknown/missing fields gracefully"
    (let [raw {:id "1" :name "Test" :unknown_field "value"}
          result (utils/map->nsmap raw "person")]
      (is (= "1" (:person/id result)))
      (is (= "value" (:person/unknown_field result)))))

  (testing "handles nil values in map"
    (let [raw {:id "1" :name nil :birth_year nil}
          result (utils/map->nsmap raw "person")]
      (is (= "1" (:person/id result)))
      (is (nil? (:person/name result)))
      (is (nil? (:person/birth_year result)))))

  (testing "handles empty strings"
    (let [raw {:id "1" :name "" :birth_year "unknown"}
          result (utils/map->nsmap raw "person")]
      (is (= "" (:person/name result)))
      (is (= "unknown" (:person/birth_year result))))))
