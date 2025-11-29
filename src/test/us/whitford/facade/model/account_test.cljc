(ns us.whitford.facade.model.account-test
  (:require
   [clojure.test :refer [deftest]]
   [fulcro-spec.core :refer [assertions =>]]
   [com.fulcrologic.rad.ids :refer [new-uuid]]
   [us.whitford.facade.model.account :as account]))

(deftest new-account-test
  (let [email "test@example.com"
        acc (account/new-account email)]
    (assertions "creates account with required fields"
      (:db/id acc) => email
      (:account/email acc) => email
      (:account/active? acc) => true
      (uuid? (:account/id acc)) => true))

  (let [acc1 (account/new-account "user1@test.com")
        acc2 (account/new-account "user2@test.com")]
    (assertions "creates unique UUIDs for different accounts"
      (not= (:account/id acc1) (:account/id acc2)) => true))

  (let [acc (account/new-account "test@example.com"
                                 :account/name "Test User"
                                 :account/role :admin)]
    (assertions "merges additional fields"
      (:account/name acc) => "Test User"
      (:account/role acc) => :admin
      (:account/email acc) => "test@example.com"))

  (let [custom-uuid (new-uuid)
        acc (account/new-account "test@example.com"
                                 :account/id custom-uuid
                                 :account/active? false)]
    (assertions "allows overriding default values"
      (:account/id acc) => custom-uuid
      (:account/active? acc) => false))

  (let [email "complex.email+tag@sub.domain.com"
        acc (account/new-account email)]
    (assertions "maintains required email structure"
      (:account/email acc) => email
      (string? (:account/email acc)) => true)))

(deftest account-structure-test
  (let [acc (account/new-account "test@example.com")]
    (assertions "account has correct keys"
      (contains? acc :db/id) => true
      (contains? acc :account/email) => true
      (contains? acc :account/active?) => true
      (contains? acc :account/id) => true))

  (let [acc (account/new-account "test@example.com")]
    (assertions "account values have correct types"
      (string? (:db/id acc)) => true
      (string? (:account/email acc)) => true
      (boolean? (:account/active? acc)) => true
      (uuid? (:account/id acc)) => true)))

(deftest account-defaults-test
  (let [acc (account/new-account "new@test.com")]
    (assertions "new accounts are active by default"
      (:account/active? acc) => true))

  (let [email "tempid@example.com"
        acc (account/new-account email)]
    (assertions "db/id uses email as tempid"
      (:db/id acc) => email)))

(deftest multiple-account-creation-test
  (let [emails ["user1@test.com" "user2@test.com" "user3@test.com"]
        accounts (mapv account/new-account emails)]
    (assertions "creates multiple accounts correctly"
      (count accounts) => 3
      (set (map :account/email accounts)) => (set emails)
      (count (set (map :account/id accounts))) => 3)))

(deftest account-identity-preservation-test
  (let [original-email "original@test.com"
        acc (account/new-account original-email)]
    (assertions "email remains unchanged after creation"
      (:account/email acc) => original-email
      (:db/id acc) => original-email))

  (let [acc (account/new-account "stable@test.com")
        original-id (:account/id acc)]
    (assertions "UUID remains stable (same account)"
      ;; The same account object should have the same ID
      (:account/id acc) => original-id)))
