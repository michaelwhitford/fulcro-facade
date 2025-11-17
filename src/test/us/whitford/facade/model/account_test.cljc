(ns us.whitford.facade.model.account-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [com.fulcrologic.rad.ids :refer [new-uuid]]
   [us.whitford.facade.model.account :as account]))

(deftest test-new-account
  (testing "creates account with required fields"
    (let [email "test@example.com"
          acc (account/new-account email)]
      (is (= email (:db/id acc)))
      (is (= email (:account/email acc)))
      (is (true? (:account/active? acc)))
      (is (uuid? (:account/id acc)))))

  (testing "creates unique UUIDs for different accounts"
    (let [acc1 (account/new-account "user1@test.com")
          acc2 (account/new-account "user2@test.com")]
      (is (not= (:account/id acc1) (:account/id acc2)))))

  (testing "merges additional fields"
    (let [acc (account/new-account "test@example.com"
                                   :account/name "Test User"
                                   :account/role :admin)]
      (is (= "Test User" (:account/name acc)))
      (is (= :admin (:account/role acc)))
      (is (= "test@example.com" (:account/email acc)))))

  (testing "allows overriding default values"
    (let [custom-uuid (new-uuid)
          acc (account/new-account "test@example.com"
                                   :account/id custom-uuid
                                   :account/active? false)]
      (is (= custom-uuid (:account/id acc)))
      (is (false? (:account/active? acc)))))

  (testing "maintains required email structure"
    (let [email "complex.email+tag@sub.domain.com"
          acc (account/new-account email)]
      (is (= email (:account/email acc)))
      (is (string? (:account/email acc))))))

(deftest test-account-structure
  (testing "account has correct keys"
    (let [acc (account/new-account "test@example.com")]
      (is (contains? acc :db/id))
      (is (contains? acc :account/email))
      (is (contains? acc :account/active?))
      (is (contains? acc :account/id))))

  (testing "account values have correct types"
    (let [acc (account/new-account "test@example.com")]
      (is (string? (:db/id acc)))
      (is (string? (:account/email acc)))
      (is (boolean? (:account/active? acc)))
      (is (uuid? (:account/id acc))))))

(deftest test-account-defaults
  (testing "new accounts are active by default"
    (let [acc (account/new-account "new@test.com")]
      (is (true? (:account/active? acc)))))

  (testing "db/id uses email as tempid"
    (let [email "tempid@example.com"
          acc (account/new-account email)]
      (is (= email (:db/id acc))))))

(deftest test-multiple-account-creation
  (testing "creates multiple accounts correctly"
    (let [emails ["user1@test.com" "user2@test.com" "user3@test.com"]
          accounts (mapv account/new-account emails)]
      (is (= 3 (count accounts)))
      (is (= (set emails) (set (map :account/email accounts))))
      (is (= 3 (count (set (map :account/id accounts))))))))

(deftest test-account-identity-preservation
  (testing "email remains unchanged after creation"
    (let [original-email "original@test.com"
          acc (account/new-account original-email)]
      (is (= original-email (:account/email acc)))
      (is (= original-email (:db/id acc)))))

  (testing "UUID remains stable (same account)"
    (let [acc (account/new-account "stable@test.com")
          original-id (:account/id acc)]
      ;; The same account object should have the same ID
      (is (= original-id (:account/id acc))))))
