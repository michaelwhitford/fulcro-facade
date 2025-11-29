(ns us.whitford.facade.components.utils-test
  (:require
   [clojure.test :refer [deftest]]
   [fulcro-spec.core :refer [assertions =>]]
   [us.whitford.facade.components.utils :as utils]))

(deftest map->nsmap-test
  (let [input {:id "1" :name "Luke"}]
    (assertions "adds namespace to simple keyword keys"
      (utils/map->nsmap input "person") => {:person/id "1" :person/name "Luke"}))

  (let [input {"string-key" "value" :keyword-key "other"}]
    (assertions "preserves non-keyword keys"
      (utils/map->nsmap input "ns") => {"string-key" "value" :ns/keyword-key "other"}))

  (assertions "handles empty map"
    (utils/map->nsmap {} "person") => {})

  (let [input {:person/id "1" :name "Luke"}]
    (assertions "preserves already namespaced keywords"
      (utils/map->nsmap input "test") => {:person/id "1" :test/name "Luke"}))

  (let [input {:id "1" :data {:nested "value"}}]
    (assertions "handles nested values correctly"
      (utils/map->nsmap input "person") => {:person/id "1" :person/data {:nested "value"}})))

(deftest map->deepnsmap-test
  (let [input {:id "1" :data {:nested "value" :deep {:key "val"}}}]
    (assertions "namespaces all keyword keys recursively"
      (utils/map->deepnsmap input "person") => {:person/id "1" :person/data {:person/nested "value" :person/deep {:person/key "val"}}}))

  (assertions "handles empty map"
    (utils/map->deepnsmap {} "person") => {})

  (let [input {:items [{:name "one"} {:name "two"}]}]
    (assertions "handles vectors of maps"
      (utils/map->deepnsmap input "item") => {:item/items [{:item/name "one"} {:item/name "two"}]})))

(deftest str->int-test
  (assertions "parses valid integer strings"
    (utils/str->int "10") => 10
    (utils/str->int "0") => 0
    (utils/str->int "-5") => -5
    (utils/str->int "999") => 999)

  (assertions "returns nil for invalid input"
    (utils/str->int "blah") => nil
    (utils/str->int "not-a-number") => nil
    (utils/str->int "") => nil)

  (assertions "returns nil for nil input"
    (utils/str->int nil) => nil)

  ;; Note: str->int throws for non-string types like vectors and maps
  ;; This is expected behavior - callers should ensure string input

  (assertions "parses with different bases"
    (utils/str->int "10" 16) => 16
    (utils/str->int "ff" 16) => 255
    (utils/str->int "111" 2) => 7))

(deftest update-in-contains-test
  (let [m {:a {:b 1}}]
    (assertions "updates when key path exists"
      (utils/update-in-contains m [:a :b] inc) => {:a {:b 2}}))

  (let [m {:a {:b 1}}]
    (assertions "leaves map unchanged when key path does not exist"
      (utils/update-in-contains m [:a :c] inc) => {:a {:b 1}}
      (utils/update-in-contains m [:x :y] inc) => {:a {:b 1}}))

  (let [m {:a 1}]
    (assertions "handles top-level keys"
      (utils/update-in-contains m [:a] inc) => {:a 2}))

  (let [m {:films ["1" "2" "3"]}]
    (assertions "handles nested structures"
      (utils/update-in-contains m [:films] count) => {:films 3})))

(deftest b64encode-and-decode-test
  (assertions "encodes and decodes round-trip"
    (utils/b64decode (utils/b64encode "Hello World")) => "Hello World"
    (utils/b64decode (utils/b64encode "Test123!@#")) => "Test123!@#")

  (assertions "encodes empty string"
    (string? (utils/b64encode "")) => true)

  (assertions "handles special characters"
    (utils/b64decode (utils/b64encode "🌟🚀")) => "🌟🚀"))

(deftest url-encode-test
  (assertions "encodes spaces"
    (string? (utils/url-encode "hello world")) => true)

  (let [encoded (utils/url-encode "Phoenix,AZ")]
    (assertions "encodes special characters"
      (string? encoded) => true
      (not= "Phoenix,AZ" encoded) => true)))

(deftest ip->hex-test
  (assertions "converts IP to hex format"
    (utils/ip->hex "10.10.10.1") => "0a0a0a01"
    (utils/ip->hex "192.168.0.1") => "c0a80001"
    (utils/ip->hex "255.255.255.255") => "ffffffff"
    (utils/ip->hex "0.0.0.0") => "00000000"))

(deftest hex->ip-test
  (assertions "converts hex back to IP"
    (utils/hex->ip "0a0a0a01") => "10.10.10.1"
    (utils/hex->ip "c0a80001") => "192.168.0.1"
    (utils/hex->ip "ffffffff") => "255.255.255.255"
    (utils/hex->ip "00000000") => "0.0.0.0")

  (assertions "round-trip conversion"
    (utils/hex->ip (utils/ip->hex "10.10.10.1")) => "10.10.10.1"
    (utils/hex->ip (utils/ip->hex "192.168.0.1")) => "192.168.0.1"))

(deftest now-test
  (assertions "returns a number"
    (number? (utils/now)) => true)

  (assertions "returns positive value"
    (pos? (utils/now)) => true))

(deftest json->data-test
  (let [json-str "{\"name\":\"Luke\",\"age\":25}"]
    (assertions "parses JSON string with keyword keys"
      (utils/json->data json-str) => {:name "Luke" :age 25}))

  (assertions "returns non-string input unchanged"
    (utils/json->data {:already "data"}) => {:already "data"}
    (utils/json->data nil) => nil
    (utils/json->data 123) => 123))
