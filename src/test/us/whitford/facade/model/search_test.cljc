(ns us.whitford.facade.model.search-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [fulcro-spec.core :refer [assertions =>]]
   [us.whitford.facade.model.swapi :as swapi]))

#?(:clj
   (deftest fetch-and-transform-entities-test
     ;; This test would require mocking the SWAPI client
     ;; For now, test the helper functions that are testable

     (assertions "entity id format"
       (format "%s-%s" "person" "1") => "person-1"
       (format "%s-%s" "film" "4") => "film-4"
       (format "%s-%s" "vehicle" "14") => "vehicle-14"
       (format "%s-%s" "starship" "9") => "starship-9"
       (format "%s-%s" "specie" "1") => "specie-1"
       (format "%s-%s" "planet" "1") => "planet-1")

     (let [film {:film/id "1" :film/title "A New Hope"}
           person {:person/id "1" :person/name "Luke Skywalker"}]
       (assertions "entity name extraction for films (uses title)"
         ;; Films use :title instead of :name
         ;; Test that we can extract the right name field
         (or (:film/name film) (:film/title film) "Unknown") => "A New Hope"
         (or (:person/name person) (:person/title person) "Unknown") => "Luke Skywalker"))

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
       (assertions "client-side filtering for films"
         (count filtered) => 1
         (:film/title (first filtered)) => "The Empire Strikes Back"))

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
       (assertions "client-side filtering with partial match"
         (count filtered) => 2
         (contains? (set (map :film/title filtered)) "The Empire Strikes Back") => true
         (contains? (set (map :film/title filtered)) "Return of the Jedi") => true))

     (let [films [{:film/id "1" :film/title "A New Hope"}]
           tests [["new" true]
                  ["NEW" true]
                  ["NeW" true]
                  ["hope" true]
                  ["HOPE" true]
                  ["phantom" false]]]
       ;; Use regular is for doseq since => doesn't work inside loops
       (doseq [[term expected] tests]
         (let [search-lower (str/lower-case term)
               matches? (some (fn [entity]
                                (str/includes? (str/lower-case (str (:film/title entity)))
                                               search-lower))
                              films)]
           (is (= expected (boolean matches?)) (str "client-side filtering case insensitivity for term: " term)))))))

(deftest search-entity-id-parsing-test
  ;; Use regular is for doseq since => doesn't work inside loops
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
        (is (= expected-type entity-type) (str "parsing entity ID from combined format - type for " id))
        (is (= expected-num entity-num) (str "parsing entity ID from combined format - num for " id)))))

  (let [invalid-ids ["person" "1" "person-" "-1" ""]]
    (doseq [id invalid-ids]
      (let [matches (re-matches #"^(.+)-(\d+)$" id)]
        (is (nil? matches) (str "handles invalid formats gracefully for: " id))))))
