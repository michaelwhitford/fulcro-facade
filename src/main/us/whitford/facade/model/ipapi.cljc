(ns us.whitford.facade.model.ipapi
  "Functions, resolvers, and mutations supporting IP geolocation API.

   DO NOT require a RAD model file in this ns. This ns is meant to be an ultimate
   leaf of the requires. Only include library code."
  (:require
   #?@(:clj [[us.whitford.facade.components.ipapi :refer [ipapi-martian]]
             [us.whitford.facade.components.config :refer [config]]])
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [com.wsscode.pathom3.connect.operation :as pco]
   [martian.core :as martian]
   [taoensso.timbre :as log]
   [us.whitford.facade.components.utils :refer [map->nsmap]]))

#?(:clj
   (defn ipapi-data
     "Fetch IP geolocation data. Returns nil on error."
     [ip-address opts]
     (try
       (let [req-opts (assoc opts :ip ip-address)
             {:keys [status body]} @(martian/response-for ipapi-martian :ip-lookup req-opts)]
         (if (= 200 status)
           (if (= "success" (:status body))
             (-> body
                 (map->nsmap "ip-info")
                 (assoc :ip-info/id ip-address))
             (do
               (log/warn "IP API returned failure status" {:ip ip-address :body body})
               nil))
           (do
             (log/error "IP API HTTP error" {:ip ip-address :status status :body body})
             nil)))
       (catch Exception e
         (log/error e "Failed to fetch IP data" {:ip ip-address :opts opts})
         nil))))

#?(:clj
   (pco/defresolver ip-info-resolver [env {:ip-info/keys [id] :as params}]
     {::pco/output [:ip-info/id :ip-info/status :ip-info/continent :ip-info/continentCode
                    :ip-info/country :ip-info/countryCode :ip-info/region :ip-info/regionName
                    :ip-info/city :ip-info/district :ip-info/zip :ip-info/lat :ip-info/lon
                    :ip-info/timezone :ip-info/offset :ip-info/currency :ip-info/isp
                    :ip-info/org :ip-info/as :ip-info/asname :ip-info/reverse
                    :ip-info/mobile :ip-info/proxy :ip-info/hosting :ip-info/query]}
     (try
       (or (ipapi-data id {}) {})
       (catch Exception e
         (log/error e "Failed to resolve ip-info" {:id id})
         {}))))

#?(:clj
   (pco/defresolver ip-info-with-lang-resolver [env {:ip-info/keys [id lang] :as params}]
     {::pco/input [:ip-info/id :ip-info/lang]
      ::pco/output [:ip-info/id :ip-info/status :ip-info/continent :ip-info/continentCode
                    :ip-info/country :ip-info/countryCode :ip-info/region :ip-info/regionName
                    :ip-info/city :ip-info/district :ip-info/zip :ip-info/lat :ip-info/lon
                    :ip-info/timezone :ip-info/offset :ip-info/currency :ip-info/isp
                    :ip-info/org :ip-info/as :ip-info/asname :ip-info/reverse
                    :ip-info/mobile :ip-info/proxy :ip-info/hosting :ip-info/query]}
     (try
       (or (ipapi-data id (cond-> {}
                            lang (assoc :lang lang))) {})
       (catch Exception e
         (log/error e "Failed to resolve ip-info with lang" {:id id :lang lang})
         {}))))

#?(:clj
   (pco/defresolver all-ip-lookups-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:ipapi/all-ip-lookups [:total {:results [:ip-info/id :ip-info/country :ip-info/city
                                                              :ip-info/lat :ip-info/lon :ip-info/isp]}]}]}
     ;; This resolver returns an empty list by default, as IP lookups are typically done one at a time
     ;; In a real app, you might store lookup history in a database
     (try
       {:ipapi/all-ip-lookups {:results []
                               :total 0}}
       (catch Exception e
         (log/error e "Failed to resolve all-ip-lookups")
         {:ipapi/all-ip-lookups {:results []
                                 :total 0}}))))

#?(:clj (def resolvers [ip-info-resolver
                        ip-info-with-lang-resolver
                        all-ip-lookups-resolver]))

(comment
  (martian/explore ipapi-martian)
  (martian/explore ipapi-martian :ip-lookup)
  @(martian/response-for ipapi-martian :ip-lookup {:ip "8.8.8.8"})
  (ipapi-data "24.48.0.1" {})
  (ipapi-data "8.8.8.8" {:lang "de"})
  (ipapi-data "1.1.1.1" {:fields "status,country,city,lat,lon"}))
