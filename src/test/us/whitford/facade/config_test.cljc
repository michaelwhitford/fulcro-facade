(ns us.whitford.facade.config-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

;; These tests validate the configuration structure without requiring the full system

(deftest test-defaults-config-structure
  (testing "defaults.edn can be loaded"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))]
      (is (map? config))
      (is (contains? config :org.httpkit.server/config))
      (is (contains? config :taoensso.timbre/logging-config))
      (is (contains? config :ring.middleware/defaults-config))))

  (testing "http-kit config has port"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          http-config (:org.httpkit.server/config config)]
      (is (map? http-config))
      (is (number? (:port http-config)))
      (is (pos? (:port http-config)))))

  (testing "datomic databases config exists"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          db-config (get config :com.fulcrologic.rad.database-adapters.datomic/databases)]
      (is (map? db-config))
      (is (contains? db-config :main))))

  (testing "swapi config exists"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          swapi-config (get config :us.whitford.facade.components.swapi/config)]
      (is (map? swapi-config))
      (is (string? (:swagger-file swapi-config)))
      (is (string? (:server-url swapi-config)))
      (is (.startsWith (:server-url swapi-config) "https://"))))

  (testing "hpapi config exists"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          hpapi-config (get config :us.whitford.facade.components.hpapi/config)]
      (is (map? hpapi-config))
      (is (string? (:swagger-file hpapi-config)))
      (is (string? (:server-url hpapi-config)))
      (is (.startsWith (:server-url hpapi-config) "https://")))))

(deftest test-pathom-config
  (testing "pathom config has logging settings"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          pathom-config (get config :com.fulcrologic.rad.pathom/config)]
      (is (map? pathom-config))
      (is (contains? pathom-config :trace?))
      (is (contains? pathom-config :log-requests?))
      (is (contains? pathom-config :log-responses?))))

  (testing "pathom config has sensitive keys"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          pathom-config (get config :com.fulcrologic.rad.pathom/config)]
      (is (set? (:sensitive-keys pathom-config)))
      (is (contains? (:sensitive-keys pathom-config) :password)))))

(deftest test-ring-middleware-config
  (testing "ring middleware has session config"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          ring-config (:ring.middleware/defaults-config config)]
      (is (map? ring-config))
      (is (true? (:session ring-config)))
      (is (true? (:cookies ring-config)))))

  (testing "ring middleware has security settings"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          security-config (get-in config [:ring.middleware/defaults-config :security])]
      (is (map? security-config))
      (is (contains? security-config :anti-forgery))
      (is (contains? security-config :hsts))
      (is (contains? security-config :xss-protection))))

  (testing "ring middleware has params config"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          params-config (get-in config [:ring.middleware/defaults-config :params])]
      (is (true? (:keywordize params-config)))
      (is (true? (:multipart params-config)))
      (is (true? (:nested params-config)))
      (is (true? (:urlencoded params-config))))))

(deftest test-timbre-logging-config
  (testing "timbre has min-level configured"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          timbre-config (get config :taoensso.timbre/logging-config)]
      (is (map? timbre-config))
      (is (keyword? (:min-level timbre-config))))))

(deftest test-datomic-main-database-config
  (testing "main database has required fields"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          main-db (get-in config [:com.fulcrologic.rad.database-adapters.datomic/databases :main])]
      (is (map? main-db))
      (is (contains? main-db :datomic/schema))
      (is (contains? main-db :datomic/client))
      (is (contains? main-db :datomic/database))))

  (testing "datomic client is dev-local type"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          client-config (get-in config [:com.fulcrologic.rad.database-adapters.datomic/databases :main :datomic/client])]
      (is (= :dev-local (:server-type client-config)))
      (is (= :mem (:storage-dir client-config))))))

(deftest test-api-endpoints
  (testing "SWAPI URL is valid"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          swapi-url (get-in config [:us.whitford.facade.components.swapi/config :server-url])]
      (is (.contains swapi-url "swapi.dev"))
      (is (.endsWith swapi-url "/api"))))

  (testing "HPAPI URL is valid"
    (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
          hpapi-url (get-in config [:us.whitford.facade.components.hpapi/config :server-url])]
      (is (.contains hpapi-url "hp-api"))
      (is (.endsWith hpapi-url "/api")))))
