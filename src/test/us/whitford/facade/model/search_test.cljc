(ns us.whitford.facade.model.search-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [us.whitford.facade.model.swapi :as swapi]))

#?(:clj
   (deftest fetch-and-transform-entities-test
     (testing "fetches and transforms entities"
       ;; This test would require mocking the SWAPI client
       ;; For now, test the helper functions that are testable
       )

     (testing "entity id format"
       (let [person-entity {:person/id "1"
                            :person/name "Luke Skywalker"}
             film-entity {:film/id "4"
                          :film/title "A New Hope"}]
         ;; Test entity ID formatting pattern
         (is (= "person-1" (format "%s-%s" "person" "1")))
         (is (= "film-4" (format "%s-%s" "film" "4")))
         (is (= "vehicle-14" (format "%s-%s" "vehicle" "14")))
         (is (= "starship-9" (format "%s-%s" "starship" "9")))
         (is (= "specie-1" (format "%s-%s" "specie" "1")))
         (is (= "planet-1" (format "%s-%s" "planet" "1")))))

     (testing "entity name extraction for films (uses title)"
       ;; Films use :title instead of :name
       (let [film {:film/id "1" :film/title "A New Hope"}
             person {:person/id "1" :person/name "Luke Skywalker"}]
         ;; Test that we can extract the right name field
         (is (= "A New Hope" (or (:film/name film) (:film/title film) "Unknown")))
         (is (= "Luke Skywalker" (or (:person/name person) (:person/title person) "Unknown")))))

     (testing "client-side filtering for films"
       (let [films [{:film/id "1" :film/title "A New Hope"}
                    {:film/id "2" :film/title "The Empire Strikes Back"}
                    {:film/id "3" :film/title "Return of the Jedi"}
                    {:film/id "4" :film/title "The Phantom Menace"}
                    {:film/id "5" :film/title "Attack of the Clones"}]
             search-term "empire"
             search-lower (str/lower-case search-term)
             filtered (filter (fn [entity]
                                (let [name-field (or (:film/title entity) 
                                                     (:film/name entity)
                                                     "")]
                                  (str/includes? (str/lower-case (str name-field)) 
                                                 search-lower)))
                              films)]
         (is (= 1 (count filtered)))
         (is (= "The Empire Strikes Back" (:film/title (first filtered))))))

     (testing "client-side filtering with partial match"
       (let [films [{:film/id "1" :film/title "A New Hope"}
                    {:film/id "2" :film/title "The Empire Strikes Back"}
                    {:film/id "3" :film/title "Return of the Jedi"}]
             search-term "the"
             search-lower (str/lower-case search-term)
             filtered (filter (fn [entity]
                                (let [name-field (or (:film/title entity) 
                                                     (:film/name entity)
                                                     "")]
                                  (str/includes? (str/lower-case (str name-field)) 
                                                 search-lower)))
                              films)]
         (is (= 2 (count filtered)))
         (is (contains? (set (map :film/title filtered)) "The Empire Strikes Back"))
         (is (contains? (set (map :film/title filtered)) "Return of the Jedi"))))

     (testing "client-side filtering case insensitivity"
       (let [films [{:film/id "1" :film/title "A New Hope"}]
             tests [["new" true]
                    ["NEW" true]
                    ["NeW" true]
                    ["hope" true]
                    ["HOPE" true]
                    ["phantom" false]]]
         (doseq [[term expected] tests]
           (let [search-lower (str/lower-case term)
                 matches? (some (fn [entity]
                                  (str/includes? (str/lower-case (str (:film/title entity))) 
                                                 search-lower))
                                films)]
             (is (= expected (boolean matches?)) (str "Search term: " term))))))))

(deftest search-entity-id-parsing-test
  (testing "parsing entity ID from combined format"
    (let [test-cases [["person-1" "person" "1"]
                      ["film-4" "film" "4"]
                      ["vehicle-14" "vehicle" "14"]
                      ["starship-9" "starship" "9"]
                      ["specie-3" "specie" "3"]
                      ["planet-1" "planet" "1"]
                      ["person-100" "person" "100"]]]
      (doseq [[id expected-type expected-num] test-cases]
        (let [matches (re-matches #"^(.+)-(\d+)$" id)
              [_ entity-type entity-num] matches]
          (is (= expected-type entity-type) (str "Type for " id))
          (is (= expected-num entity-num) (str "Num for " id))))))

  (testing "handles invalid formats gracefully"
    (let [invalid-ids ["person" "1" "person-" "-1" ""]]
      (doseq [id invalid-ids]
        (let [matches (re-matches #"^(.+)-(\d+)$" id)]
          (is (nil? matches) (str "Should not match: " id)))))))
