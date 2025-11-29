(ns us.whitford.facade.config-test
  (:require
   [clojure.test :refer [deftest]]
   [fulcro-spec.core :refer [assertions =>]]
   [clojure.edn :as edn]
   #?(:clj [clojure.java.io :as io])))

;; These tests validate the configuration structure without requiring the full system

#?(:clj
   (deftest defaults-config-structure-test
     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))]
       (assertions "defaults.edn can be loaded"
         (map? config) => true
         (contains? config :org.httpkit.server/config) => true
         (contains? config :taoensso.timbre/logging-config) => true
         (contains? config :ring.middleware/defaults-config) => true))

     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           http-config (:org.httpkit.server/config config)]
       (assertions "http-kit config has port"
         (map? http-config) => true
         (number? (:port http-config)) => true
         (pos? (:port http-config)) => true))

     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           db-config (get config :com.fulcrologic.rad.database-adapters.datomic/databases)]
       (assertions "datomic databases config exists"
         (map? db-config) => true
         (contains? db-config :main) => true))

     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           swapi-config (get config :us.whitford.facade.components.swapi/config)]
       (assertions "swapi config exists"
         (map? swapi-config) => true
         (string? (:swagger-file swapi-config)) => true
         (string? (:server-url swapi-config)) => true
         (.startsWith (:server-url swapi-config) "https://") => true))

     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           hpapi-config (get config :us.whitford.facade.components.hpapi/config)]
       (assertions "hpapi config exists"
         (map? hpapi-config) => true
         (string? (:swagger-file hpapi-config)) => true
         (string? (:server-url hpapi-config)) => true
         (.startsWith (:server-url hpapi-config) "https://") => true))))

#?(:clj
   (deftest pathom-config-test
     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           pathom-config (get config :com.fulcrologic.rad.pathom/config)]
       (assertions "pathom config has logging settings"
         (map? pathom-config) => true
         (contains? pathom-config :trace?) => true
         (contains? pathom-config :log-requests?) => true
         (contains? pathom-config :log-responses?) => true))

     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           pathom-config (get config :com.fulcrologic.rad.pathom/config)]
       (assertions "pathom config has sensitive keys"
         (set? (:sensitive-keys pathom-config)) => true
         (contains? (:sensitive-keys pathom-config) :password) => true))))

#?(:clj
   (deftest ring-middleware-config-test
     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           ring-config (:ring.middleware/defaults-config config)]
       (assertions "ring middleware has session config"
         (map? ring-config) => true
         (:session ring-config) => true
         (:cookies ring-config) => true))

     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           security-config (get-in config [:ring.middleware/defaults-config :security])]
       (assertions "ring middleware has security settings"
         (map? security-config) => true
         (contains? security-config :anti-forgery) => true
         (contains? security-config :hsts) => true
         (contains? security-config :xss-protection) => true))

     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           params-config (get-in config [:ring.middleware/defaults-config :params])]
       (assertions "ring middleware has params config"
         (:keywordize params-config) => true
         (:multipart params-config) => true
         (:nested params-config) => true
         (:urlencoded params-config) => true))))

#?(:clj
   (deftest timbre-logging-config-test
     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           timbre-config (get config :taoensso.timbre/logging-config)]
       (assertions "timbre has min-level configured"
         (map? timbre-config) => true
         (keyword? (:min-level timbre-config)) => true))))

#?(:clj
   (deftest datomic-main-database-config-test
     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           main-db (get-in config [:com.fulcrologic.rad.database-adapters.datomic/databases :main])]
       (assertions "main database has required fields"
         (map? main-db) => true
         (contains? main-db :datomic/schema) => true
         (contains? main-db :datomic/client) => true
         (contains? main-db :datomic/database) => true))

     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           client-config (get-in config [:com.fulcrologic.rad.database-adapters.datomic/databases :main :datomic/client])]
       (assertions "datomic client is dev-local type"
         (:server-type client-config) => :dev-local
         (:storage-dir client-config) => :mem))))

#?(:clj
   (deftest api-endpoints-test
     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           swapi-url (get-in config [:us.whitford.facade.components.swapi/config :server-url])]
       (assertions "SWAPI URL is valid"
         (.contains swapi-url "swapi.dev") => true
         (.endsWith swapi-url "/api") => true))

     (let [config (edn/read-string (slurp (io/resource "config/defaults.edn")))
           hpapi-url (get-in config [:us.whitford.facade.components.hpapi/config :server-url])]
       (assertions "HPAPI URL is valid"
         (.contains hpapi-url "hp-api") => true
         (.endsWith hpapi-url "/api") => true))))
