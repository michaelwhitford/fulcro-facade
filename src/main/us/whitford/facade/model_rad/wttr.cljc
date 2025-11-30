(ns us.whitford.facade.model-rad.wttr
  "RAD definition for wttr.in Weather API. Attributes only."
  (:require
    [clojure.spec.alpha :as spec]
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.form-options :as fo]))

;; Weather attributes (current conditions + location)

(defattr weather_id :weather/id :string
  {ao/identity? true
   ao/required? true
   ao/schema :production})

(defattr weather_temp-c :weather/temp-c :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_temp-f :weather/temp-f :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_feels-like-c :weather/feels-like-c :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_feels-like-f :weather/feels-like-f :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_humidity :weather/humidity :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_cloud-cover :weather/cloud-cover :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_uv-index :weather/uv-index :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_visibility :weather/visibility :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_pressure :weather/pressure :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_precip-mm :weather/precip-mm :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_wind-speed-kmph :weather/wind-speed-kmph :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_wind-speed-mph :weather/wind-speed-mph :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_wind-dir :weather/wind-dir :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_wind-degree :weather/wind-degree :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_description :weather/description :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_observation-time :weather/observation-time :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_local-obs-time :weather/local-obs-time :string
  {ao/identities #{:weather/id}
   ao/schema :production})

;; Location attributes

(defattr weather_area-name :weather/area-name :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_country :weather/country :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_region :weather/region :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_latitude :weather/latitude :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_longitude :weather/longitude :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_population :weather/population :string
  {ao/identities #{:weather/id}
   ao/schema :production})

;; Astronomy attributes (included in weather entity)

(defattr weather_sunrise :weather/sunrise :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_sunset :weather/sunset :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_moonrise :weather/moonrise :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_moonset :weather/moonset :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_moon-phase :weather/moon-phase :string
  {ao/identities #{:weather/id}
   ao/schema :production})

(defattr weather_moon-illumination :weather/moon-illumination :string
  {ao/identities #{:weather/id}
   ao/schema :production})

;; Forecast ref (nested daily forecasts)

(defattr weather_forecast :weather/forecast :ref
  {ao/identities #{:weather/id}
   ao/target :weather-day/date
   ao/cardinality :many
   ao/schema :production})

;; Weather Day attributes (daily forecast)

(defattr weather-day_date :weather-day/date :string
  {ao/identity? true
   ao/required? true
   ao/schema :production})

(defattr weather-day_max-temp-c :weather-day/max-temp-c :string
  {ao/identities #{:weather-day/date}
   ao/schema :production})

(defattr weather-day_max-temp-f :weather-day/max-temp-f :string
  {ao/identities #{:weather-day/date}
   ao/schema :production})

(defattr weather-day_min-temp-c :weather-day/min-temp-c :string
  {ao/identities #{:weather-day/date}
   ao/schema :production})

(defattr weather-day_min-temp-f :weather-day/min-temp-f :string
  {ao/identities #{:weather-day/date}
   ao/schema :production})

(defattr weather-day_avg-temp-c :weather-day/avg-temp-c :string
  {ao/identities #{:weather-day/date}
   ao/schema :production})

(defattr weather-day_avg-temp-f :weather-day/avg-temp-f :string
  {ao/identities #{:weather-day/date}
   ao/schema :production})

(defattr weather-day_sun-hour :weather-day/sun-hour :string
  {ao/identities #{:weather-day/date}
   ao/schema :production})

(defattr weather-day_total-snow-cm :weather-day/total-snow-cm :string
  {ao/identities #{:weather-day/date}
   ao/schema :production})

(defattr weather-day_uv-index :weather-day/uv-index :string
  {ao/identities #{:weather-day/date}
   ao/schema :production})

(def weather-attributes
  [weather_id
   weather_temp-c weather_temp-f
   weather_feels-like-c weather_feels-like-f
   weather_humidity weather_cloud-cover weather_uv-index
   weather_visibility weather_pressure weather_precip-mm
   weather_wind-speed-kmph weather_wind-speed-mph
   weather_wind-dir weather_wind-degree
   weather_description weather_observation-time weather_local-obs-time
   weather_area-name weather_country weather_region
   weather_latitude weather_longitude weather_population
   weather_sunrise weather_sunset
   weather_moonrise weather_moonset
   weather_moon-phase weather_moon-illumination
   weather_forecast])

(def weather-day-attributes
  [weather-day_date
   weather-day_max-temp-c weather-day_max-temp-f
   weather-day_min-temp-c weather-day_min-temp-f
   weather-day_avg-temp-c weather-day_avg-temp-f
   weather-day_sun-hour weather-day_total-snow-cm
   weather-day_uv-index])

(def attributes (concat weather-attributes weather-day-attributes))
