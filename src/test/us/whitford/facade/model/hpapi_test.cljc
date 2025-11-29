(ns us.whitford.facade.model.hpapi-test
  (:require
   [clojure.string]
   [clojure.test :refer [deftest]]
   [fulcro-spec.core :refer [assertions =>]]
   [us.whitford.facade.components.utils :as utils]))

;; Note: Since hpapi.cljc uses server-only features (#?(:clj ...)),
;; we test the data transformation utilities that are shared

(deftest character-data-transformation-test
  (let [raw-character {:id "9e3f7ce4-b9a7-4244-b709-dae5c1f1d4a8"
                       :name "Harry Potter"
                       :species "human"
                       :gender "male"
                       :house "Gryffindor"
                       :dateOfBirth "31-07-1980"
                       :wizard true
                       :ancestry "half-blood"
                       :eyeColour "green"
                       :hairColour "black"
                       :patronus "stag"
                       :hogwartsStudent true
                       :hogwartsStaff false
                       :actor "Daniel Radcliffe"
                       :alive true
                       :image "https://ik.imagekit.io/hpapi/harry.jpg"}
        expected {:character/id "9e3f7ce4-b9a7-4244-b709-dae5c1f1d4a8"
                  :character/name "Harry Potter"
                  :character/species "human"
                  :character/gender "male"
                  :character/house "Gryffindor"
                  :character/dateOfBirth "31-07-1980"
                  :character/wizard true
                  :character/ancestry "half-blood"
                  :character/eyeColour "green"
                  :character/hairColour "black"
                  :character/patronus "stag"
                  :character/hogwartsStudent true
                  :character/hogwartsStaff false
                  :character/actor "Daniel Radcliffe"
                  :character/alive true
                  :character/image "https://ik.imagekit.io/hpapi/harry.jpg"}]
    (assertions "transforms raw character API data to namespaced map"
      (utils/map->nsmap raw-character "character") => expected))

  (let [raw {:id "123" :name "Unknown"}
        expected {:character/id "123" :character/name "Unknown"}]
    (assertions "handles minimal character data"
      (utils/map->nsmap raw "character") => expected))

  (let [raw {:id "456" :name "Mysterious" :house nil :patronus nil}
        result (utils/map->nsmap raw "character")]
    (assertions "handles character with nil values"
      (:character/id result) => "456"
      (:character/name result) => "Mysterious"
      (:character/house result) => nil
      (:character/patronus result) => nil)))

(deftest spell-data-transformation-test
  (let [raw-spell {:id "c76a2922-ba4c-4278-baab-44defb631236"
                   :name "Expelliarmus"
                   :description "Disarming Charm"}
        expected {:spell/id "c76a2922-ba4c-4278-baab-44defb631236"
                  :spell/name "Expelliarmus"
                  :spell/description "Disarming Charm"}]
    (assertions "transforms raw spell API data to namespaced map"
      (utils/map->nsmap raw-spell "spell") => expected))

  (let [raw {:id "spell-1"
             :name "Avada Kedavra"
             :description "The Killing Curse, one of three Unforgivable Curses"}
        result (utils/map->nsmap raw "spell")]
    (assertions "handles spell with longer description"
      (:spell/id result) => "spell-1"
      (:spell/name result) => "Avada Kedavra"
      (string? (:spell/description result)) => true)))

(deftest filtering-characters-by-search-test
  (let [characters [{:character/id "1" :character/name "Harry Potter"}
                    {:character/id "2" :character/name "Hermione Granger"}
                    {:character/id "3" :character/name "Ron Weasley"}
                    {:character/id "4" :character/name "Draco Malfoy"}]
        search "Harry"
        filtered (filterv #(clojure.string/includes? (:character/name %) search) characters)]
    (assertions "filters characters by partial name match"
      (count filtered) => 1
      (:character/name (first filtered)) => "Harry Potter"))

  (let [characters [{:character/id "1" :character/name "Harry Potter"}
                    {:character/id "2" :character/name "Hagrid"}]
        search "harry"
        filtered (filterv #(clojure.string/includes? (:character/name %) search) characters)]
    (assertions "filters characters case-sensitive"
      (count filtered) => 0))

  (let [characters [{:character/id "1" :character/name "Harry Potter"}]
        search "Gandalf"
        filtered (filterv #(clojure.string/includes? (:character/name %) search) characters)]
    (assertions "returns empty when no match"
      (empty? filtered) => true)))

(deftest house-filtering-test
  (let [characters [{:character/id "1" :character/name "Harry" :character/house "Gryffindor"}
                    {:character/id "2" :character/name "Draco" :character/house "Slytherin"}
                    {:character/id "3" :character/name "Luna" :character/house "Ravenclaw"}
                    {:character/id "4" :character/name "Cedric" :character/house "Hufflepuff"}]
        gryffindors (filterv #(= "Gryffindor" (:character/house %)) characters)]
    (assertions "filters characters by house"
      (count gryffindors) => 1
      (:character/name (first gryffindors)) => "Harry"))

  (let [characters [{:character/id "1" :character/name "Harry" :character/house "Gryffindor"}
                    {:character/id "2" :character/name "Muggle" :character/house nil}]
        with-house (filterv #(some? (:character/house %)) characters)]
    (assertions "handles characters without house"
      (count with-house) => 1)))

(deftest hogwarts-role-filtering-test
  (let [characters [{:character/id "1" :character/name "Harry" :character/hogwartsStudent true :character/hogwartsStaff false}
                    {:character/id "2" :character/name "Snape" :character/hogwartsStudent false :character/hogwartsStaff true}]
        students (filterv :character/hogwartsStudent characters)]
    (assertions "filters students only"
      (count students) => 1
      (:character/name (first students)) => "Harry"))

  (let [characters [{:character/id "1" :character/name "Harry" :character/hogwartsStudent true :character/hogwartsStaff false}
                    {:character/id "2" :character/name "Snape" :character/hogwartsStudent false :character/hogwartsStaff true}]
        staff (filterv :character/hogwartsStaff characters)]
    (assertions "filters staff only"
      (count staff) => 1
      (:character/name (first staff)) => "Snape")))
