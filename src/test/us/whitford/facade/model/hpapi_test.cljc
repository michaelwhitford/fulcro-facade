(ns us.whitford.facade.model.hpapi-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [us.whitford.facade.components.utils :as utils]))

;; Note: Since hpapi.cljc uses server-only features (#?(:clj ...)),
;; we test the data transformation utilities that are shared

(deftest test-character-data-transformation
  (testing "transforms raw character API data to namespaced map"
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
      (is (= expected (utils/map->nsmap raw-character "character")))))

  (testing "handles minimal character data"
    (let [raw {:id "123" :name "Unknown"}
          expected {:character/id "123" :character/name "Unknown"}]
      (is (= expected (utils/map->nsmap raw "character")))))

  (testing "handles character with nil values"
    (let [raw {:id "456" :name "Mysterious" :house nil :patronus nil}
          result (utils/map->nsmap raw "character")]
      (is (= "456" (:character/id result)))
      (is (= "Mysterious" (:character/name result)))
      (is (nil? (:character/house result)))
      (is (nil? (:character/patronus result))))))

(deftest test-spell-data-transformation
  (testing "transforms raw spell API data to namespaced map"
    (let [raw-spell {:id "c76a2922-ba4c-4278-baab-44defb631236"
                     :name "Expelliarmus"
                     :description "Disarming Charm"}
          expected {:spell/id "c76a2922-ba4c-4278-baab-44defb631236"
                    :spell/name "Expelliarmus"
                    :spell/description "Disarming Charm"}]
      (is (= expected (utils/map->nsmap raw-spell "spell")))))

  (testing "handles spell with longer description"
    (let [raw {:id "spell-1"
               :name "Avada Kedavra"
               :description "The Killing Curse, one of three Unforgivable Curses"}
          result (utils/map->nsmap raw "spell")]
      (is (= "spell-1" (:spell/id result)))
      (is (= "Avada Kedavra" (:spell/name result)))
      (is (string? (:spell/description result))))))

(deftest test-filtering-characters-by-search
  (testing "filters characters by partial name match"
    (let [characters [{:character/id "1" :character/name "Harry Potter"}
                      {:character/id "2" :character/name "Hermione Granger"}
                      {:character/id "3" :character/name "Ron Weasley"}
                      {:character/id "4" :character/name "Draco Malfoy"}]
          search "Harry"
          filtered (filterv #(clojure.string/includes? (:character/name %) search) characters)]
      (is (= 1 (count filtered)))
      (is (= "Harry Potter" (:character/name (first filtered))))))

  (testing "filters characters case-sensitive"
    (let [characters [{:character/id "1" :character/name "Harry Potter"}
                      {:character/id "2" :character/name "Hagrid"}]
          search "harry"
          filtered (filterv #(clojure.string/includes? (:character/name %) search) characters)]
      (is (= 0 (count filtered)))))

  (testing "returns empty when no match"
    (let [characters [{:character/id "1" :character/name "Harry Potter"}]
          search "Gandalf"
          filtered (filterv #(clojure.string/includes? (:character/name %) search) characters)]
      (is (empty? filtered)))))

(deftest test-house-filtering
  (testing "filters characters by house"
    (let [characters [{:character/id "1" :character/name "Harry" :character/house "Gryffindor"}
                      {:character/id "2" :character/name "Draco" :character/house "Slytherin"}
                      {:character/id "3" :character/name "Luna" :character/house "Ravenclaw"}
                      {:character/id "4" :character/name "Cedric" :character/house "Hufflepuff"}]
          gryffindors (filterv #(= "Gryffindor" (:character/house %)) characters)]
      (is (= 1 (count gryffindors)))
      (is (= "Harry" (:character/name (first gryffindors))))))

  (testing "handles characters without house"
    (let [characters [{:character/id "1" :character/name "Harry" :character/house "Gryffindor"}
                      {:character/id "2" :character/name "Muggle" :character/house nil}]
          with-house (filterv #(some? (:character/house %)) characters)]
      (is (= 1 (count with-house))))))

(deftest test-hogwarts-role-filtering
  (testing "filters students only"
    (let [characters [{:character/id "1" :character/name "Harry" :character/hogwartsStudent true :character/hogwartsStaff false}
                      {:character/id "2" :character/name "Snape" :character/hogwartsStudent false :character/hogwartsStaff true}]
          students (filterv :character/hogwartsStudent characters)]
      (is (= 1 (count students)))
      (is (= "Harry" (:character/name (first students))))))

  (testing "filters staff only"
    (let [characters [{:character/id "1" :character/name "Harry" :character/hogwartsStudent true :character/hogwartsStaff false}
                      {:character/id "2" :character/name "Snape" :character/hogwartsStudent false :character/hogwartsStaff true}]
          staff (filterv :character/hogwartsStaff characters)]
      (is (= 1 (count staff)))
      (is (= "Snape" (:character/name (first staff)))))))
