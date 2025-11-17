(ns us.whitford.facade.components.utils-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [us.whitford.facade.components.utils :as utils]))

(deftest test-map->nsmap
  (testing "adds namespace to simple keyword keys"
    (let [input {:id "1" :name "Luke"}]
      (is (= {:person/id "1" :person/name "Luke"}
             (utils/map->nsmap input "person")))))

  (testing "preserves non-keyword keys"
    (let [input {"string-key" "value" :keyword-key "other"}]
      (is (= {"string-key" "value" :ns/keyword-key "other"}
             (utils/map->nsmap input "ns")))))

  (testing "handles empty map"
    (is (= {} (utils/map->nsmap {} "person"))))

  (testing "preserves already namespaced keywords"
    (let [input {:person/id "1" :name "Luke"}]
      (is (= {:person/id "1" :test/name "Luke"}
             (utils/map->nsmap input "test")))))

  (testing "handles nested values correctly"
    (let [input {:id "1" :data {:nested "value"}}]
      (is (= {:person/id "1" :person/data {:nested "value"}}
             (utils/map->nsmap input "person"))))))

(deftest test-map->deepnsmap
  (testing "namespaces all keyword keys recursively"
    (let [input {:id "1" :data {:nested "value" :deep {:key "val"}}}]
      (is (= {:person/id "1" :person/data {:person/nested "value" :person/deep {:person/key "val"}}}
             (utils/map->deepnsmap input "person")))))

  (testing "handles empty map"
    (is (= {} (utils/map->deepnsmap {} "person"))))

  (testing "handles vectors of maps"
    (let [input {:items [{:name "one"} {:name "two"}]}]
      (is (= {:item/items [{:item/name "one"} {:item/name "two"}]}
             (utils/map->deepnsmap input "item"))))))

(deftest test-str->int
  (testing "parses valid integer strings"
    (is (= 10 (utils/str->int "10")))
    (is (= 0 (utils/str->int "0")))
    (is (= -5 (utils/str->int "-5")))
    (is (= 999 (utils/str->int "999"))))

  (testing "returns nil for invalid input"
    (is (nil? (utils/str->int "blah")))
    (is (nil? (utils/str->int "not-a-number")))
    (is (nil? (utils/str->int ""))))

  (testing "returns nil for nil input"
    (is (nil? (utils/str->int nil))))
  
  ;; Note: str->int throws for non-string types like vectors and maps
  ;; This is expected behavior - callers should ensure string input

  (testing "parses with different bases"
    (is (= 16 (utils/str->int "10" 16)))
    (is (= 255 (utils/str->int "ff" 16)))
    (is (= 7 (utils/str->int "111" 2)))))

(deftest test-update-in-contains
  (testing "updates when key path exists"
    (let [m {:a {:b 1}}]
      (is (= {:a {:b 2}} (utils/update-in-contains m [:a :b] inc)))))

  (testing "leaves map unchanged when key path does not exist"
    (let [m {:a {:b 1}}]
      (is (= {:a {:b 1}} (utils/update-in-contains m [:a :c] inc)))
      (is (= {:a {:b 1}} (utils/update-in-contains m [:x :y] inc)))))

  (testing "handles top-level keys"
    (let [m {:a 1}]
      (is (= {:a 2} (utils/update-in-contains m [:a] inc)))))

  (testing "handles nested structures"
    (let [m {:films ["1" "2" "3"]}]
      (is (= {:films 3} (utils/update-in-contains m [:films] count))))))

(deftest test-b64encode-and-decode
  (testing "encodes and decodes round-trip"
    (is (= "Hello World" (utils/b64decode (utils/b64encode "Hello World"))))
    (is (= "Test123!@#" (utils/b64decode (utils/b64encode "Test123!@#")))))

  (testing "encodes empty string"
    (is (string? (utils/b64encode ""))))

  (testing "handles special characters"
    (is (= "🌟🚀" (utils/b64decode (utils/b64encode "🌟🚀"))))))

(deftest test-url-encode
  (testing "encodes spaces"
    (is (string? (utils/url-encode "hello world"))))

  (testing "encodes special characters"
    (let [encoded (utils/url-encode "Phoenix,AZ")]
      (is (string? encoded))
      (is (not= "Phoenix,AZ" encoded)))))

(deftest test-ip->hex
  (testing "converts IP to hex format"
    (is (= "0a0a0a01" (utils/ip->hex "10.10.10.1")))
    (is (= "c0a80001" (utils/ip->hex "192.168.0.1")))
    (is (= "ffffffff" (utils/ip->hex "255.255.255.255")))
    (is (= "00000000" (utils/ip->hex "0.0.0.0")))))

(deftest test-hex->ip
  (testing "converts hex back to IP"
    (is (= "10.10.10.1" (utils/hex->ip "0a0a0a01")))
    (is (= "192.168.0.1" (utils/hex->ip "c0a80001")))
    (is (= "255.255.255.255" (utils/hex->ip "ffffffff")))
    (is (= "0.0.0.0" (utils/hex->ip "00000000"))))

  (testing "round-trip conversion"
    (is (= "10.10.10.1" (utils/hex->ip (utils/ip->hex "10.10.10.1"))))
    (is (= "192.168.0.1" (utils/hex->ip (utils/ip->hex "192.168.0.1"))))))

(deftest test-now
  (testing "returns a number"
    (is (number? (utils/now))))

  (testing "returns positive value"
    (is (pos? (utils/now)))))

(deftest test-json->data
  (testing "parses JSON string with keyword keys"
    (let [json-str "{\"name\":\"Luke\",\"age\":25}"]
      (is (= {:name "Luke" :age 25} (utils/json->data json-str)))))

  (testing "returns non-string input unchanged"
    (is (= {:already "data"} (utils/json->data {:already "data"})))
    (is (= nil (utils/json->data nil)))
    (is (= 123 (utils/json->data 123)))))
