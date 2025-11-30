(ns us.whitford.facade.model.wttr
  "Functions, resolvers, and mutations supporting wttr.in Weather API.

   DO NOT require a RAD model file in this ns. This ns is meant to be an ultimate
   leaf of the requires. Only include library code."
  (:require
   #?@(:clj [[us.whitford.facade.components.wttr :refer [wttr-martian]]
             [us.whitford.facade.components.config :refer [config]]])
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [com.wsscode.pathom3.connect.operation :as pco]
   [martian.core :as martian]
   [taoensso.timbre :as log]
   [us.whitford.facade.components.utils :refer [map->nsmap]]))

#?(:clj
   (defn extract-value
     "Extract :value from wttr's [{:value \"...\"}] format"
     [v]
     (when (and (sequential? v) (first v))
       (:value (first v)))))

#?(:clj
   (defn transform-current-condition
     "Transform current condition data to namespaced keywords"
     [condition]
     (when condition
       {:weather/temp-c (:temp_C condition)
        :weather/temp-f (:temp_F condition)
        :weather/feels-like-c (:FeelsLikeC condition)
        :weather/feels-like-f (:FeelsLikeF condition)
        :weather/humidity (:humidity condition)
        :weather/cloud-cover (:cloudcover condition)
        :weather/uv-index (:uvIndex condition)
        :weather/visibility (:visibility condition)
        :weather/pressure (:pressure condition)
        :weather/precip-mm (:precipMM condition)
        :weather/wind-speed-kmph (:windspeedKmph condition)
        :weather/wind-speed-mph (:windspeedMiles condition)
        :weather/wind-dir (:winddir16Point condition)
        :weather/wind-degree (:winddirDegree condition)
        :weather/description (extract-value (:weatherDesc condition))
        :weather/observation-time (:observation_time condition)
        :weather/local-obs-time (:localObsDateTime condition)})))

#?(:clj
   (defn transform-area
     "Transform nearest area data to namespaced keywords"
     [area]
     (when area
       {:weather/area-name (extract-value (:areaName area))
        :weather/country (extract-value (:country area))
        :weather/region (extract-value (:region area))
        :weather/latitude (:latitude area)
        :weather/longitude (:longitude area)
        :weather/population (:population area)})))

#?(:clj
   (defn transform-astronomy
     "Transform astronomy data to namespaced keywords"
     [astronomy]
     (when astronomy
       {:weather/sunrise (:sunrise astronomy)
        :weather/sunset (:sunset astronomy)
        :weather/moonrise (:moonrise astronomy)
        :weather/moonset (:moonset astronomy)
        :weather/moon-phase (:moon_phase astronomy)
        :weather/moon-illumination (:moon_illumination astronomy)})))

#?(:clj
   (defn transform-daily-forecast
     "Transform daily weather data to namespaced keywords"
     [day]
     (when day
       (merge
        {:weather-day/date (:date day)
         :weather-day/max-temp-c (:maxtempC day)
         :weather-day/max-temp-f (:maxtempF day)
         :weather-day/min-temp-c (:mintempC day)
         :weather-day/min-temp-f (:mintempF day)
         :weather-day/avg-temp-c (:avgtempC day)
         :weather-day/avg-temp-f (:avgtempF day)
         :weather-day/sun-hour (:sunHour day)
         :weather-day/total-snow-cm (:totalSnow_cm day)
         :weather-day/uv-index (:uvIndex day)}
        (when-let [astro (first (:astronomy day))]
          (transform-astronomy astro))))))

#?(:clj
   (defn wttr-data
     "Fetch weather data from wttr.in. Returns nil on error."
     [location opts]
     (try
       (let [req-opts (merge {:location location :format "j1"} opts)
             {:keys [status body]} @(martian/response-for wttr-martian :forecast req-opts)]
         (if (= 200 status)
           (let [current (first (:current_condition body))
                 area (first (:nearest_area body))
                 forecast (:weather body)]
             (merge
              {:weather/id location}
              (transform-current-condition current)
              (transform-area area)
              {:weather/forecast (mapv transform-daily-forecast forecast)}))
           (do
             (log/error "wttr.in HTTP error" {:location location :status status :body body})
             nil)))
       (catch Exception e
         (log/error e "Failed to fetch weather data" {:location location :opts opts})
         nil))))

(def weather-output
  "Common output spec for weather data"
  [:weather/id
   :weather/temp-c :weather/temp-f
   :weather/feels-like-c :weather/feels-like-f
   :weather/humidity :weather/cloud-cover :weather/uv-index
   :weather/visibility :weather/pressure :weather/precip-mm
   :weather/wind-speed-kmph :weather/wind-speed-mph
   :weather/wind-dir :weather/wind-degree
   :weather/description :weather/observation-time :weather/local-obs-time
   :weather/area-name :weather/country :weather/region
   :weather/latitude :weather/longitude :weather/population
   {:weather/forecast [:weather-day/date
                       :weather-day/max-temp-c :weather-day/max-temp-f
                       :weather-day/min-temp-c :weather-day/min-temp-f
                       :weather-day/avg-temp-c :weather-day/avg-temp-f
                       :weather-day/sun-hour :weather-day/total-snow-cm
                       :weather-day/uv-index
                       :weather/sunrise :weather/sunset
                       :weather/moonrise :weather/moonset
                       :weather/moon-phase :weather/moon-illumination]}])

#?(:clj
   (pco/defresolver weather-resolver [env {:weather/keys [id] :as params}]
     {::pco/output weather-output}
     (try
       (or (wttr-data id {}) {})
       (catch Exception e
         (log/error e "Failed to resolve weather" {:id id})
         {}))))

#?(:clj
   (pco/defresolver weather-from-ip-resolver
     "Bridge resolver: derives weather data from IP geolocation.
      Given :ip-info/city, provides all weather attributes.
      This allows querying weather directly from an IP address:
      [{[:ip-info/id \"8.8.8.8\"] [:weather/temp-c :weather/description]}]"
     [env {:ip-info/keys [city] :as params}]
     {::pco/input [:ip-info/city]
      ::pco/output weather-output}
     (try
       (if (and city (not (str/blank? city)))
         (or (wttr-data city {}) {})
         (do
           (log/warn "Cannot get weather: no city from IP lookup")
           {}))
       (catch Exception e
         (log/error e "Failed to resolve weather from IP" {:city city})
         {}))))

#?(:clj (def resolvers [weather-resolver weather-from-ip-resolver]))

(comment
  (martian/explore wttr-martian)
  (martian/explore wttr-martian :forecast)
  @(martian/response-for wttr-martian :forecast {:location "London" :format "j1"})
  (wttr-data "London" {})
  (wttr-data "New+York" {})
  (wttr-data "Tokyo" {}))
