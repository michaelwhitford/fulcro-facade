(ns us.whitford.facade.ui.search-forms-test
  (:require
   [clojure.test :refer [deftest]]
   [fulcro-spec.core :refer [assertions =>]]
   [us.whitford.facade.ui.search-forms :as sf]))

(deftest entity-type-icon-test
  (assertions "returns correct icon for each entity type"
    (sf/entity-type-icon :person) => "user"
    (sf/entity-type-icon :film) => "film"
    (sf/entity-type-icon :vehicle) => "car"
    (sf/entity-type-icon :starship) => "space shuttle"
    (sf/entity-type-icon :specie) => "hand spock"
    (sf/entity-type-icon :planet) => "globe")

  (assertions "returns question mark for unknown types"
    (sf/entity-type-icon :unknown) => "question"
    (sf/entity-type-icon :other) => "question"
    (sf/entity-type-icon nil) => "question"))
