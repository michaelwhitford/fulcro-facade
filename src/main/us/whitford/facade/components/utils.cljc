(ns us.whitford.facade.components.utils
  (:require
    #?(:clj [clojure.data.json :as json])
    #?(:cljs [goog.crypt.base64 :as b64])
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [clojure.walk :refer [postwalk]])
  #?(:clj (:import
            [java.util Base64]
            [java.net URLEncoder])))

#?(:clj (set! *warn-on-reflection* true))

(defn now
  "generate current time as seconds since unix epoch"
  [& _]
  #?(:clj (quot (System/currentTimeMillis) 1000)
     :cljs (.now js/Date)))

(defn url-encode
  "url encode the string"
  #?(:clj ([^String s] (URLEncoder/encode s "UTF-8"))
     :cljs ([s] (js/encodeURIComponent s))))

(comment
  (url-encode "Phoenix,AZ"))

(defn ip->hex
  "encode ip to hex format"
  [ip]
  #?(:clj
     (let [octets (str/split ^String ip #"\.")]
       (str/join (mapv #(format "%02x" (read-string %)) octets)))
     :cljs
     (let [octets (str/split ip #"\.")]
       (str/join (mapv #(.padStart (.toString (js/parseInt %) 16) 2 "0") octets)))))

(comment
  (ip->hex "10.10.10.1") ; => "0a0a0a01"
  )

(defn hex->ip
  "decode hex to ip string"
  [hex]
  #?(:clj
     (let [octets (mapv #(str/join %) (partition 2 ^String hex))]
       (str/join "." (mapv #(read-string (format "0x%s" %)) octets)))
     :cljs
     (let [octets (mapv #(str/join %) (partition 2 hex))]
       (str/join "." (mapv #(js/parseInt % 16) octets)))))

(comment
  (hex->ip "0a0a0a01") ; => "10.10.10.1"
  )

(defn b64encode
  "encode string to base64"
  [s]
  #?(:clj
     (let [encoder (Base64/getEncoder)]
       (.encodeToString encoder (.getBytes ^String s)))
     :cljs
     (b64/encodeString s)))

(defn b64decode
  "decode base64 string to string"
  ([s]
   #?(:clj
      (let [decoder (Base64/getDecoder)]
        (String. (.decode decoder ^String s)))
      :cljs
      (b64/decodeString s))))

(defn json->data
  "convert json string to data"
  [s]
  (if (string? s)
      #?(:clj
         (json/read-str s {:key-fn keyword})
         :cljs
         (js->clj (.parse js/JSON s) :keywordize-keys true))
      s))

(defn update-in-contains
  "update-in when map contains keys"
  [m ks f]
  (cond-> m
          (get-in m ks) (update-in ks f)))

(defn map->deepnsmap
  "Apply the string s to the map m as a namespace. Walk all keys."
  [m s]
  (postwalk
    (fn [x]
      (if (keyword? x)
          (keyword s (name x))
          x))
    m))

(defn map->nsmap
  "Apply the string s to the map m as a namespace. Top level keys only."
  [m s]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                     (not (qualified-keyword? k)))
                                (keyword (str s) (name k))
                                k)]
                 (assoc acc new-kw v)))
    {} m))

(defn str->int
  ([s] (str->int s 10))
  ([s b]
   (try
     #?(:clj (Integer/parseInt s b)
        :cljs (let [i (js/parseInt s b)]
                (if (js/isNaN i)
                    nil
                    i)))
     (catch #?(:clj NumberFormatException :cljs :default) e nil))))

(comment
  (str->int 1) ; => 1
  (str->int "blah") ; => nil
  (str->int "10") ; => 10
  (str->int [1 2 3]) ; => nil
  )
