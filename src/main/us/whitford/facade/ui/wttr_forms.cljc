(ns us.whitford.facade.ui.wttr-forms
  (:require
   #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom]
      :cljs [com.fulcrologic.fulcro.dom :as dom])
   [clojure.string :as str]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.data-fetch :refer [load!]]
   [com.fulcrologic.fulcro.mutations :as mutations :refer [defmutation]]
   [com.fulcrologic.rad.control :as control]
   [com.fulcrologic.rad.form :as form]
   [com.fulcrologic.rad.form-options :as fo]
   [com.fulcrologic.rad.report :as report]
   [com.fulcrologic.rad.report-options :as ro]
   [com.fulcrologic.statecharts.integration.fulcro.rad-integration :as ri]
   [taoensso.timbre :as log]
   [us.whitford.facade.model-rad.wttr :as rm.wttr])
  #?(:cljs (:require-macros [us.whitford.facade.ui.wttr-forms])))

;; Forecast day component
(defsc ForecastDay [this {:weather-day/keys [date max-temp-c min-temp-c avg-temp-c sun-hour]
                          :weather/keys [sunrise sunset moon-phase] :as props}]
  {:query [:weather-day/date
           :weather-day/max-temp-c :weather-day/max-temp-f
           :weather-day/min-temp-c :weather-day/min-temp-f
           :weather-day/avg-temp-c :weather-day/avg-temp-f
           :weather-day/sun-hour :weather-day/total-snow-cm :weather-day/uv-index
           :weather/sunrise :weather/sunset
           :weather/moonrise :weather/moonset
           :weather/moon-phase :weather/moon-illumination]
   :ident :weather-day/date}
  (dom/div :.ui.card
           (dom/div :.content
                    (dom/div :.header date)
                    (dom/div :.meta
                             (dom/span (str "High: " max-temp-c "°C / Low: " min-temp-c "°C")))
                    (dom/div :.description
                             (dom/p (str "Average: " avg-temp-c "°C"))
                             (dom/p (str "Sun: " sunrise " - " sunset))
                             (dom/p (str "Moon: " moon-phase))
                             (dom/p (str "Sun hours: " sun-hour))))))

(def ui-forecast-day (comp/factory ForecastDay {:keyfn :weather-day/date}))

;; Weather detail form
(form/defsc-form WeatherForm [this {:weather/keys [id area-name country region
                                                    temp-c temp-f feels-like-c feels-like-f
                                                    humidity cloud-cover uv-index
                                                    visibility pressure precip-mm
                                                    wind-speed-kmph wind-speed-mph wind-dir
                                                    description observation-time local-obs-time
                                                    latitude longitude population
                                                    forecast] :as props}]
  {fo/id             rm.wttr/weather_id
   fo/title          "Weather Forecast"
   fo/route-prefix   "weather"
   fo/attributes     [rm.wttr/weather_id
                      rm.wttr/weather_area-name
                      rm.wttr/weather_country
                      rm.wttr/weather_region
                      rm.wttr/weather_temp-c
                      rm.wttr/weather_temp-f
                      rm.wttr/weather_feels-like-c
                      rm.wttr/weather_feels-like-f
                      rm.wttr/weather_humidity
                      rm.wttr/weather_cloud-cover
                      rm.wttr/weather_uv-index
                      rm.wttr/weather_visibility
                      rm.wttr/weather_pressure
                      rm.wttr/weather_precip-mm
                      rm.wttr/weather_wind-speed-kmph
                      rm.wttr/weather_wind-speed-mph
                      rm.wttr/weather_wind-dir
                      rm.wttr/weather_description
                      rm.wttr/weather_observation-time
                      rm.wttr/weather_local-obs-time
                      rm.wttr/weather_latitude
                      rm.wttr/weather_longitude]
   fo/cancel-route   ::WeatherLookupWidget
   fo/read-only?     true
   fo/layout         :default})

(def ui-weather-form (comp/factory WeatherForm))

;; Query component for loading weather via IP (Pathom connects ip-info -> weather)
(defsc WeatherByIP [this props]
  {:query [:ip-info/id :ip-info/city :ip-info/regionName :ip-info/country
           :weather/id :weather/temp-c :weather/temp-f
           :weather/feels-like-c :weather/feels-like-f
           :weather/humidity :weather/cloud-cover :weather/uv-index
           :weather/visibility :weather/pressure :weather/precip-mm
           :weather/wind-speed-kmph :weather/wind-speed-mph
           :weather/wind-dir :weather/wind-degree
           :weather/description :weather/observation-time :weather/local-obs-time
           :weather/area-name :weather/country :weather/region
           :weather/latitude :weather/longitude
           {:weather/forecast (comp/get-query ForecastDay)}]
   :ident :ip-info/id})

#?(:cljs
   (defn fetch-user-ip!
     "Fetches user's public IP from ipify.org and calls callback with the IP string"
     [callback]
     (-> (js/fetch "https://api.ipify.org?format=json")
         (.then (fn [response] (.json response)))
         (.then (fn [^js data]
                  (let [ip (.-ip data)]
                    (log/info "Got user IP:" ip)
                    (callback ip))))
         (.catch (fn [err]
                   (log/error "Failed to get user IP:" err)
                   nil)))))

;; Weather lookup widget (main entry point)
;; Note: We use a dynamic query for :result since it can be nil or hold weather data
(defsc WeatherLookupWidget [this {:keys [location loading-ip?] :as props}]
  {:query [:location :loading-ip?
           ;; result is a join - can be either weather or ip-info depending on how loaded
           {:result (comp/get-query WeatherByIP)}]
   :ident (fn [] [:component/id ::WeatherLookupWidget])
   :route-segment ["weather-lookup"]
   :initial-state {:location "" :loading-ip? false}}
  (let [result (get props :result)
        {:weather/keys [id area-name country region
                        temp-c feels-like-c humidity wind-speed-kmph wind-dir
                        description local-obs-time forecast]
         :ip-info/keys [city]} result
        ;; Use area-name from weather if available, otherwise city from ip-info
        display-area (or area-name city)]
    (dom/div :.ui.segment
             (dom/h2 "Weather Forecast")
             (dom/div :.ui.form
                      (dom/div :.field
                               (dom/label "Location")
                               (dom/input {:type "text"
                                           :value (or location "")
                                           :placeholder "Enter city name (e.g., London, New York, Tokyo)"
                                           :onChange (fn [e]
                                                       (mutations/set-string! this :location :event e))
                                           :onKeyPress (fn [e]
                                                         #?(:cljs
                                                            (when (= "Enter" (.-key e))
                                                              (when (and location (not (str/blank? location)))
                                                                (let [loc (str/replace location " " "+")]
                                                                  (log/info "Looking up weather for:" loc)
                                                                  (load! this [:weather/id loc] WeatherForm
                                                                         {:target [:component/id ::WeatherLookupWidget :result]}))))))}))
                      (dom/div :.ui.buttons
                               (dom/button :.ui.primary.button
                                           {:onClick (fn []
                                                       #?(:cljs
                                                          (when (and location (not (str/blank? location)))
                                                            (let [loc (str/replace location " " "+")]
                                                              (log/info "Looking up weather for:" loc)
                                                              (load! this [:weather/id loc] WeatherForm
                                                                     {:target [:component/id ::WeatherLookupWidget :result]})))))}
                                           "Get Weather")
                               (dom/div :.or)
                               (dom/button :.ui.secondary.button
                                           {:className (when loading-ip? "loading disabled")
                                            :onClick (fn []
                                                       #?(:cljs
                                                          (do
                                                            (mutations/set-value! this :loading-ip? true)
                                                            (fetch-user-ip!
                                                             (fn [ip]
                                                               (log/info "Loading weather for IP:" ip)
                                                               ;; Pathom will connect: ip-info -> weather via the bridge resolver
                                                               (load! this [:ip-info/id ip] WeatherByIP
                                                                      {:target [:component/id ::WeatherLookupWidget :result]
                                                                       :post-action (fn [_]
                                                                                      (mutations/set-value! this :loading-ip? false))}))))))}
                                           (dom/i :.location.arrow.icon)
                                           "Use My Location")))

             ;; Show results if we have them
             (when result
               (dom/div :.ui.segment {:style {:marginTop "1em"}}
                        (dom/h3 (str display-area
                                     (when region (str ", " region))
                                     (when country (str ", " country))))
                        (dom/p :.ui.large.text (str description))

                        ;; Current conditions card
                        (dom/div :.ui.cards
                                 (dom/div :.ui.card
                                          (dom/div :.content
                                                   (dom/div :.header "Current Conditions")
                                                   (dom/div :.description
                                                            (dom/p (dom/strong "Temperature: ")
                                                                   (str temp-c "°C (feels like " feels-like-c "°C)"))
                                                            (dom/p (dom/strong "Humidity: ") (str humidity "%"))
                                                            (dom/p (dom/strong "Wind: ")
                                                                   (str wind-speed-kmph " km/h " wind-dir))
                                                            (dom/p (dom/strong "Observed: ") local-obs-time)))))

                        ;; Forecast cards
                        (when (seq forecast)
                          (dom/div {:style {:marginTop "1em"}}
                                   (dom/h4 "3-Day Forecast")
                                   (dom/div :.ui.three.cards
                                            (map ui-forecast-day forecast)))))))))

(def ui-weather-lookup-widget (comp/factory WeatherLookupWidget))
