(ns us.whitford.facade.ui.ipapi-forms
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
   [us.whitford.facade.model-rad.ipapi :as ri.ipapi])
  #?(:cljs (:require-macros [us.whitford.facade.ui.ipapi-forms])))

(form/defsc-form IpInfoForm [this {:ip-info/keys [id query status country countryCode region
                                                   regionName city district zip lat lon timezone
                                                   offset currency isp org as asname reverse
                                                   mobile proxy hosting continent continentCode] :as props}]
  {fo/id             ri.ipapi/ip-info_id
   fo/title          "IP Geolocation Info"
   fo/route-prefix   "ip-info"
   fo/attributes     [ri.ipapi/ip-info_id
                      ri.ipapi/ip-info_query
                      ri.ipapi/ip-info_status
                      ri.ipapi/ip-info_country
                      ri.ipapi/ip-info_countryCode
                      ri.ipapi/ip-info_region
                      ri.ipapi/ip-info_regionName
                      ri.ipapi/ip-info_city
                      ri.ipapi/ip-info_district
                      ri.ipapi/ip-info_zip
                      ri.ipapi/ip-info_lat
                      ri.ipapi/ip-info_lon
                      ri.ipapi/ip-info_timezone
                      ri.ipapi/ip-info_offset
                      ri.ipapi/ip-info_currency
                      ri.ipapi/ip-info_isp
                      ri.ipapi/ip-info_org
                      ri.ipapi/ip-info_as
                      ri.ipapi/ip-info_asname
                      ri.ipapi/ip-info_reverse
                      ri.ipapi/ip-info_mobile
                      ri.ipapi/ip-info_proxy
                      ri.ipapi/ip-info_hosting
                      ri.ipapi/ip-info_continent
                      ri.ipapi/ip-info_continentCode]
   fo/cancel-route   ::IpLookupList
   fo/read-only?     true
   fo/layout         :default})

(def ui-ip-info-form (comp/factory IpInfoForm))

(defsc IpInfoListItem [this
                       {:ip-info/keys [id query country city lat lon isp] :as props}
                       {:keys [report-instance row-class ::report/idx]}]
  {:query [:ip-info/id :ip-info/query :ip-info/country :ip-info/city
           :ip-info/lat :ip-info/lon :ip-info/isp]
   :ident :ip-info/id}
  (let [{:keys [edit-form entity-id]} (report/form-link report-instance props :ip-info/id)]
    (dom/div :.item
             (dom/div :.content
                      (if edit-form
                        (dom/a :.link.header {:onClick (fn [] (ri/edit! this edit-form entity-id))}
                               (str query " - " city ", " country))
                        (dom/div :.header (str query " - " city ", " country)))))))

(def ui-ip-info-list-item (comp/factory IpInfoListItem))

(report/defsc-report IpLookupList [this props]
  {ro/title "IP Lookups"
   ro/route "ip-lookups"
   ro/source-attribute    :ipapi/all-ip-lookups
   ro/row-pk              ri.ipapi/ip-info_id
   ro/columns             [ri.ipapi/ip-info_query ri.ipapi/ip-info_country
                           ri.ipapi/ip-info_city ri.ipapi/ip-info_isp]
   ro/column-formatters   {:ip-info/query (fn [this v {:ip-info/keys [id] :as p}]
                                             (dom/a {:onClick #(ri/edit! this IpInfoForm id)} (str v)))}
   ro/form-links          {ri.ipapi/ip-info_id IpInfoForm}
   ro/row-actions         [{::report/label "View Details"
                            ::report/action (fn [report-instance {:ip-info/keys [id]}]
                                              (ri/edit! report-instance IpInfoForm id))}]})

(def ui-ip-lookup-list (comp/factory IpLookupList))

;; Simple IP lookup component
(defsc IpLookupWidget [this {:keys [ip-address]}]
  {:query [:ip-address]
   :ident (fn [] [:component/id ::IpLookupWidget])
   :route-segment ["ip-lookup"]
   :initial-state {:ip-address "8.8.8.8"}}
  (dom/div :.ui.segment
           (dom/h3 "IP Geolocation Lookup")
           (dom/div :.ui.form
                    (dom/div :.field
                             (dom/label "IP Address")
                             (dom/input {:type "text"
                                         :value ip-address
                                         :placeholder "Enter IP address (e.g., 8.8.8.8)"
                                         :onChange (fn [e]
                                                     (mutations/set-string! this :ip-address :event e))}))
                    (dom/button :.ui.primary.button
                                {:onClick (fn []
                                            #?(:cljs
                                               (when (and ip-address (not (str/blank? ip-address)))
                                                 (log/info "Looking up IP:" ip-address)
                                                 (load! this [:ip-info/id ip-address] IpInfoForm
                                                        {:post-action (fn [{:keys [app]}]
                                                                        (ri/edit! app IpInfoForm ip-address))}))))}
                                "Lookup"))))

(def ui-ip-lookup-widget (comp/factory IpLookupWidget))
