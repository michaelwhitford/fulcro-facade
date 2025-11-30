(ns us.whitford.facade.model-rad.ipapi
  "RAD definition for IP geolocation API. Attributes only."
  (:require
    [clojure.spec.alpha :as spec]
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.form-options :as fo]))

;; IP Info attributes

(defattr ip-info_id :ip-info/id :string
  {ao/identity? true
   ao/required? true
   ao/schema :production})

(defattr ip-info_status :ip-info/status :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_message :ip-info/message :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_continent :ip-info/continent :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_continentCode :ip-info/continentCode :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_country :ip-info/country :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_countryCode :ip-info/countryCode :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_region :ip-info/region :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_regionName :ip-info/regionName :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_city :ip-info/city :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_district :ip-info/district :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_zip :ip-info/zip :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_lat :ip-info/lat :double
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_lon :ip-info/lon :double
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_timezone :ip-info/timezone :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_offset :ip-info/offset :int
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_currency :ip-info/currency :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_isp :ip-info/isp :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_org :ip-info/org :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_as :ip-info/as :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_asname :ip-info/asname :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_reverse :ip-info/reverse :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_mobile :ip-info/mobile :boolean
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_proxy :ip-info/proxy :boolean
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_hosting :ip-info/hosting :boolean
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_query :ip-info/query :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

(defattr ip-info_lang :ip-info/lang :string
  {ao/identities #{:ip-info/id}
   ao/schema :production})

;; Collection attributes

(defattr all-ip-lookups :ipapi/all-ip-lookups :ref
  {ao/target :ip-info/id
   ao/pc-output [{:ipapi/all-ip-lookups [:total {:results '...}]}]
   ao/pc-resolve :ipapi/all-ip-lookups})

(def ip-info-attributes
  [ip-info_id ip-info_status ip-info_message ip-info_continent ip-info_continentCode
   ip-info_country ip-info_countryCode ip-info_region ip-info_regionName
   ip-info_city ip-info_district ip-info_zip ip-info_lat ip-info_lon
   ip-info_timezone ip-info_offset ip-info_currency ip-info_isp ip-info_org
   ip-info_as ip-info_asname ip-info_reverse ip-info_mobile ip-info_proxy
   ip-info_hosting ip-info_query ip-info_lang])

(def attributes (concat ip-info-attributes [all-ip-lookups]))
