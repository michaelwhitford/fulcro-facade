(ns us.whitford.facade.ui.search-forms-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [us.whitford.facade.ui.search-forms :as sf]))

(deftest entity-type-icon-test
  (testing "returns correct icon for each entity type"
    (is (= "user" (sf/entity-type-icon :person)))
    (is (= "film" (sf/entity-type-icon :film)))
    (is (= "car" (sf/entity-type-icon :vehicle)))
    (is (= "space shuttle" (sf/entity-type-icon :starship)))
    (is (= "hand spock" (sf/entity-type-icon :specie)))
    (is (= "globe" (sf/entity-type-icon :planet))))

  (testing "returns question mark for unknown types"
    (is (= "question" (sf/entity-type-icon :unknown)))
    (is (= "question" (sf/entity-type-icon :other)))
    (is (= "question" (sf/entity-type-icon nil)))))
