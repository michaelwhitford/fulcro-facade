(ns us.whitford.facade.client
  (:require
    [com.fulcrologic.devtools.common.target :refer [ido]]
    [com.fulcrologic.fulcro.algorithms.timbre-support :refer [console-appender prefix-output-fn]]
    [com.fulcrologic.fulcro.algorithms.tx-processing.batched-processing :as btxn]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.networking.http-remote :as net]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.rad.application :as rad-app]
    [com.fulcrologic.rad.database-adapters.datomic-options :as do]
    [com.fulcrologic.rad.rendering.semantic-ui.semantic-ui-controls :as sui]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.routing :as routing]
    [com.fulcrologic.rad.routing.history :as history]
    [com.fulcrologic.rad.routing.html5-history :as hist5 :refer [new-html5-history]]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [com.fulcrologic.statecharts :as sc]
    [com.fulcrologic.statecharts.chart :refer [statechart]]
    [com.fulcrologic.statecharts.elements :refer [state on-entry on-exit]]
    [com.fulcrologic.statecharts.integration.fulcro :as scf]
    [com.fulcrologic.statecharts.integration.fulcro.rad-integration :as ri]
    [com.fulcrologic.statecharts.integration.fulcro.ui-routes :as uir ]
    [fulcro.inspect.tool :as it]
    [goog.functions :refer [debounce]]
    [taoensso.timbre :as log]
    [us.whitford.facade.application :refer [SPA]]
    [us.whitford.facade.ui.account-forms :refer [AccountForm AccountList]]
    [us.whitford.facade.ui.hpapi-forms :refer [CharacterForm CharacterList
                                               SpellForm SpellList]]
    [us.whitford.facade.ui.ipapi-forms :refer [IpLookupWidget IpLookupList IpInfoForm]]
    [us.whitford.facade.ui.wttr-forms :refer [WeatherLookupWidget WeatherForm]]
    [us.whitford.facade.ui.root :refer [LandingPage Root]]
    [us.whitford.facade.ui.search-forms :refer [SearchReport]]
    [us.whitford.facade.ui.swapi-forms :refer [PersonForm PersonList Person
                                               FilmForm FilmList
                                               PlanetForm PlanetList
                                               SpeciesForm SpeciesList
                                               VehicleForm VehicleList
                                               StarshipForm StarshipList]]
    [us.whitford.facade.ui.toast :as toast]
    [us.whitford.facade.model.agent-comms :as agent-comms]
    [us.whitford.fulcro-radar.api :as radar]))

(defn setup-RAD [app]
  (rad-app/install-ui-controls! app sui/all-controls)
  (report/install-formatter! app :boolean :affirmation (fn [_ value] (if value "yes" "no"))))

(defn wrap-error-reporting []
  (let [debounced-toast! (debounce toast/toast! 1000)]
    (fn error-reporting [{:keys [body status-code error outgoing-request] :as response}]
      (when (not= 200 status-code)
            (debounced-toast! "There was an error.  Please try again."))
      response)))

(def response-middleware (-> (wrap-error-reporting) (net/wrap-fulcro-response)))

(def application-chart
  (statechart {:name "fulcro-swapi"}
    (uir/routing-regions
      (uir/routes {:id :region/routes
                   :routing/root Root}
        (state {:id :state/running}
          (uir/rstate {:route/target `LandingPage
                       :route/path   ["landing-page"]})
          (ri/report-state {:route/target `AccountList
                            :route/path   ["accounts"]})
          (ri/form-state {:route/target `AccountForm
                          :route/path  ["account"]})
          (ri/report-state {:route/target `PersonList
                            :route/path   ["people"]})
          (ri/form-state {:route/target `PersonForm
                          :route/path    ["person"]})
          (ri/report-state {:route/target `FilmList
                            :route/path   ["films"]})
          (ri/form-state {:route/target `FilmForm
                          :route/path    ["film"]})
          (ri/report-state {:route/target `PlanetList
                            :route/path   ["planets"]})
          (ri/form-state {:route/target `PlanetForm
                          :route/path    ["planet"]})
          (ri/report-state {:route/target `SpeciesList
                            :route/path   ["species"]})
          (ri/form-state {:route/target `SpeciesForm
                          :route/path    ["specie"]})
          (ri/report-state {:route/target `VehicleList
                            :route/path   ["vehicles"]})
          (ri/form-state {:route/target `VehicleForm
                          :route/path    ["vehicle"]})
          (ri/report-state {:route/target `StarshipList
                            :route/path   ["starships"]})
          (ri/form-state {:route/target `StarshipForm
                          :route/path    ["starship"]})
          (ri/report-state {:route/target `SearchReport
                            :route/path   ["search"]})
          (ri/report-state {:route/target `CharacterList
                            :route/path   ["characters"]})
          (ri/form-state {:route/target `CharacterForm
                          :route/path    ["character"]})
          (ri/report-state {:route/target `SpellList
                            :route/path   ["spells"]})
          (ri/form-state {:route/target `SpellForm
                          :route/path    ["spell"]})
          (uir/rstate {:route/target `IpLookupWidget
                       :route/path   ["ip-lookup"]})
          (ri/report-state {:route/target `IpLookupList
                            :route/path   ["ip-lookups"]})
          (ri/form-state {:route/target `IpInfoForm
                          :route/path    ["ip-info"]})
          ;; Weather (wttr.in)
          (uir/rstate {:route/target `WeatherLookupWidget
                       :route/path   ["weather-lookup"]})
          (ri/form-state {:route/target `WeatherForm
                          :route/path    ["weather"]})
          )))))

(defonce app (-> (rad-app/fulcro-rad-app
                   (let [token (when-not (undefined? js/fulcro_network_csrf_token)
                                 js/fulcro_network_csrf_token)]
                     {:remotes
                      {:remote (net/fulcro-http-remote {:url "/api"
                                                        ; add middleware and use `toast!` for errors
                                                        :response-middleware response-middleware
                                                        :request-middleware (rad-app/secured-request-middleware {:csrf-token token})})
                       #_#_:render-middleware (when goog.DEBUG js/holyjak.fulcro_troubleshooting.troubleshooting_render_middleware)}}))
                 (btxn/with-batched-reads)))

(defn refresh []
  ;; hot code reload of installed controls
  (log/info "Reinstalling controls")
  (setup-RAD app)
  ;; Ensure statecharts are installed on refresh
  (when-not (::scf/statechart-registry app)
    (log/info "Reinstalling statecharts registry")
    (scf/install-fulcro-statecharts! app))
  (uir/update-chart! app application-chart)
  #_(comp/refresh-dynamic-queries! app)
  (app/force-root-render! app))


(m/defmutation application-ready [_]
  (action [{:keys [state]}]
    (log/info "Setting application ready state to true")
    (swap! state assoc :ui/ready? true)
    (log/info "Application ready state set")))

(defn init []
  ;; makes js console logging a bit nicer
  (log/merge-config! {:output-fn prefix-output-fn
                      :appenders {:console (console-appender)}})
  (log/info "Starting App")
  (reset! SPA app)
  #_(tap> {:from ::init :spa SPA})
  #_(history/install-route-history! app (html5-history))
  ;; default time zone (can be changed at login for given user)
  (datetime/set-timezone! "America/Phoenix")
  (app/set-root! app Root {:initialize-state? true})
  (setup-RAD app)
  (scf/install-fulcro-statecharts! app)
  (log/info "Statecharts installed:" (boolean (::scf/statechart-registry app)))
  ;; dynamic routing
  ;; (dr/initialize! app)
  ;; (dr/change-route! app ["landing-page"])
  ;; (history/install-route-history! app (new-html5-history {:app app :default-route {:route ["landing-page"]}}))
  (app/mount! app Root "app" {:initialize-state? false})
  (log/info "App mounted, starting routing...")
  (try
    (uir/start-routing! app application-chart)
    (log/info "Routing started, navigating to LandingPage...")
    (uir/route-to! app `LandingPage)
    (log/info "Navigation complete, setting application ready...")
    (comp/transact! app [(application-ready {})])
    (log/info "Application ready transaction submitted")
    (catch :default e
      (log/error "Error during routing initialization:" e)
      ;; Still mark as ready so user can see something rather than infinite loading
      (comp/transact! app [(application-ready {})])
      (log/warn "Application marked ready despite routing error")))
  ;; (hist5/restore-route! app LandingPage {})
  (ido (it/add-fulcro-inspect! app)))

(comment
  (radar/send-diagnostic! SPA)
  )
