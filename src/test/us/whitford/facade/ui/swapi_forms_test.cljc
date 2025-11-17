(ns us.whitford.facade.ui.swapi-forms-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [us.whitford.facade.ui.swapi-forms :as swapi-forms]))

(deftest test-calculate-page-count
  (testing "calculates page count correctly"
    (is (= 1 (swapi-forms/calculate-page-count 10 10)))
    (is (= 2 (swapi-forms/calculate-page-count 11 10)))
    (is (= 2 (swapi-forms/calculate-page-count 20 10)))
    (is (= 9 (swapi-forms/calculate-page-count 82 10)))
    (is (= 9 (swapi-forms/calculate-page-count 87 10)))
    (is (= 4 (swapi-forms/calculate-page-count 37 10))))

  (testing "handles edge cases"
    (is (= 1 (swapi-forms/calculate-page-count 0 10)))
    (is (= 1 (swapi-forms/calculate-page-count 1 10)))
    (is (= 10 (swapi-forms/calculate-page-count 100 10))))

  (testing "handles nil values"
    (is (= 1 (swapi-forms/calculate-page-count nil 10)))
    (is (= 1 (swapi-forms/calculate-page-count 100 nil)))
    (is (= 1 (swapi-forms/calculate-page-count nil nil))))

  (testing "handles zero page size"
    (is (= 1 (swapi-forms/calculate-page-count 100 0))))

  (testing "handles different page sizes"
    (is (= 5 (swapi-forms/calculate-page-count 25 5)))
    (is (= 4 (swapi-forms/calculate-page-count 100 25)))
    (is (= 2 (swapi-forms/calculate-page-count 100 50)))))
