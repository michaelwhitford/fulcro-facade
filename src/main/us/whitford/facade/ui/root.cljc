(ns us.whitford.facade.ui.root
  "App UI root. Standard Fulcro."
  (:require
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom]
       :cljs [com.fulcrologic.fulcro.dom :as dom])
    #?@(:cljs [[com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown :refer [ui-dropdown]]
               [com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown-menu :refer [ui-dropdown-menu]]
               [com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown-item :refer [ui-dropdown-item]]
               [com.fulcrologic.semantic-ui.modules.modal.ui-modal-content :refer [ui-modal-content]]
               [com.fulcrologic.semantic-ui.modules.modal.ui-modal-actions :refer [ui-modal-actions]]])
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [com.fulcrologic.rad.routing :as rroute]
    [com.fulcrologic.semantic-ui.modules.modal.ui-modal :refer [ui-modal]]
    [com.fulcrologic.statecharts.integration.fulcro :as scf]
    [com.fulcrologic.statecharts.integration.fulcro.rad-integration :as ri]
    [com.fulcrologic.statecharts.integration.fulcro.ui-routes :as uir]
    [taoensso.timbre :as log]
    [us.whitford.facade.ui.account-forms :refer [AccountForm AccountList]]
    [us.whitford.facade.ui.hpapi-forms :refer [CharacterList CharacterForm
                                               SpellList SpellForm]]
    [us.whitford.facade.ui.ipapi-forms :refer [IpLookupList IpInfoForm IpLookupWidget]]
    [us.whitford.facade.ui.wttr-forms :refer [WeatherLookupWidget WeatherForm]]
    [us.whitford.facade.ui.search-forms :refer [Search ui-search SearchReport]]
    [us.whitford.facade.ui.swapi-forms :refer [PersonList PersonForm Person
                                               FilmList FilmForm
                                               PlanetList PlanetForm
                                               SpeciesList SpeciesForm
                                               VehicleList VehicleForm
                                               StarshipList StarshipForm]]

    [us.whitford.facade.ui.toast :as toast]
    [us.whitford.facade.ui.game :as game]))

(defsc LandingPage [this props]
  {:query         ['*]
   :ident         (fn [] [:component/id ::LandingPage])
   :initial-state {}
   :route-segment ["landing-page"]
   :use-hooks? true}
  (dom/div {}
    (dom/h1 :.ui.header 
      (dom/i :.robot.icon)
      (dom/div :.content 
        "Facade"
        (dom/div :.sub.header "A self-building Fulcro RAD application")))
    
    (dom/div :.ui.three.stackable.cards {:style {:marginTop "2em"}}
      ;; APIs Card
      (dom/div :.ui.card
        (dom/div :.content
          (dom/div :.header (dom/i :.database.icon) " Integrated APIs")
          (dom/div :.description
            (dom/div :.ui.list
              (dom/div :.item (dom/i :.star.icon) " Star Wars (SWAPI)")
              (dom/div :.item (dom/i :.magic.icon) " Harry Potter")
              (dom/div :.item (dom/i :.map.marker.icon) " IP Geolocation")
              (dom/div :.item (dom/i :.cloud.icon) " Weather Forecast")))))
      
      ;; Features Card
      (dom/div :.ui.card
        (dom/div :.content
          (dom/div :.header (dom/i :.cogs.icon) " Built With")
          (dom/div :.description
            (dom/div :.ui.list
              (dom/div :.item (dom/i :.react.icon) " Fulcro + RAD")
              (dom/div :.item (dom/i :.project.diagram.icon) " Pathom3 Resolvers")
              (dom/div :.item (dom/i :.exchange.icon) " Martian HTTP Clients")
              (dom/div :.item (dom/i :.sitemap.icon) " Statechart Routing")))))
      
      ;; Games Card  
      (dom/div :.ui.card
        (dom/div :.content
          (dom/div :.header (dom/i :.gamepad.icon) " Toast Games")
          (dom/div :.description
            (dom/p {} "Try the games in the top-right menu!")
            (dom/div :.ui.list
              (dom/div :.item (dom/i :.bullseye.icon) " Whack-a-Toast!")
              (dom/div :.item (dom/i :.th.icon) " Tic-Tac-Toast!"))))))
    
    (dom/div :.ui.message {:style {:marginTop "2em"}}
      (dom/div :.header "🤖 AI-Assisted Development")
      (dom/p {} 
        "This app is designed to be extended by AI agents. Point an AI at the repo and say "
        (dom/em {} "\"add support for X API\"")
        " — and it works.")
      (dom/p {}
        (dom/strong {} "Requirements: ")
        "An AI agent with REPL access via "
        (dom/a {:href "https://github.com/bhauman/clojure-mcp" :target "_blank"} "clojure-mcp")
        " or "
        (dom/a {:href "https://github.com/bhauman/clojure-mcp-light" :target "_blank"} "clojure-mcp-light")
        ".")
      (dom/p {}
        "See "
        (dom/code {} "AGENTS.md")
        " for the integration guide."))))



(defsc Root [this {::app/keys [active-remotes]
                   :ui/keys   [ready? search current-route]}]
  {:query         [::app/active-remotes (scf/statechart-session-ident uir/session-id)
                   :ui/ready?
                   {:ui/search (comp/get-query Search)}
                   ;; The :ui/current-route will be dynamically updated by the statechart router
                   ;; Start with a placeholder that will be replaced
                   {:ui/current-route (comp/get-query LandingPage)}
                   ]
   :initial-state (fn [params] {:ui/ready? false
                                :ui/search (comp/get-initial-state Search)
                                :ui/current-route {}
                                })}
  (let [config (scf/current-configuration this uir/session-id)
        routing-blocked? (uir/route-denied? this)
        busy? (seq active-remotes)]
    #_(tap> {:from ::Root :config config :routing-blocked? routing-blocked? :busy? busy?})
    #?(:cljs
       (dom/div
         (ui-modal {:open routing-blocked?}
           (ui-modal-content {}
             "Routing away from this form will lose unsaved changes.  Are you sure?")
           (ui-modal-actions {}
             (dom/button :.ui.negative.button {:onClick (fn [] (uir/abandon-route-change! this))} "No")
             (dom/button :.ui.positive.button {:onClick (fn [] (uir/force-continue-routing! this))} "Yes")))
         (comp/fragment
           (toast/ui-toast-container)
           (if ready?
               (dom/div
                 (dom/div :.ui.top.menu
                   (dom/div :.ui.item.link (dom/a {:onClick (fn [] #_(rroute/route-to! this LandingPage {})
                                                              (uir/route-to! this `LandingPage))} "Home"))
                   (ui-dropdown {:className "item" :text "Account"}
                     (ui-dropdown-menu {}
                       (ui-dropdown-item {:onClick (fn [] #_(rroute/route-to! this AccountList {})
                                                     (uir/route-to! this `AccountList {})
                                                     )}"View All")
                       (ui-dropdown-item {:onClick (fn [] #_(form/create! this AccountForm)
                                                     (ri/create! this `AccountForm))} "New")))
                   (ui-dropdown {:className "item" :text "Star Wars"}
                     (ui-dropdown-menu {}
                       (ui-dropdown-item {:onClick (fn [] #_(rroute/route-to! this FilmList {})
                                                     (uir/route-to! this `FilmList {}))}
                         (dom/i :.compact.ui.left.floated.film.icon " Films"))
                       (ui-dropdown-item {:onClick (fn [] #_(rroute/route-to! this PersonList {})
                                                     (uir/route-to! this `PersonList {}))}
                         (dom/i :.compact.ui.left.floated.users.icon " People"))
                       (ui-dropdown-item {:onClick (fn [] #_(rroute/route-to! this PlanetList {})
                                                     (uir/route-to! this `PlanetList {}))}
                         (dom/i :.compact.ui.left.floated.globe.icon " Planets"))
                       (ui-dropdown-item {:onClick (fn [] #_(rroute/route-to! this SpeciesList {})
                                                     (uir/route-to! this `SpeciesList {}))}
                         (dom/i :.compact.ui.left.floated.hand.spock.icon " Species"))
                       (ui-dropdown-item {:onClick (fn [] #_(rroute/route-to! this StarshipList {})
                                                     (uir/route-to! this `StarshipList {}))}
                         (dom/i :.compact.ui.left.floated.space.shuttle.icon " Starships"))
                       (ui-dropdown-item {:onClick (fn [] #_(rroute/route-to! this VehicleList {})
                                                     (uir/route-to! this `VehicleList {}))}
                         (dom/i :.compacte.ui.left.floated.car.icon " Vehicles"))
                       (ui-dropdown-item {:onClick (fn [] #_(rroute/route-to! this SearchReport {})
                                                     (uir/route-to! this `SearchReport {}))} "Search")
                       ))
                   (ui-dropdown {:className "item" :text "Harry Potter"}
                     (ui-dropdown-menu {}
                       (ui-dropdown-item {:onClick (fn [] (uir/route-to! this `CharacterList {}))}
                         (dom/i :.compact.ui.left.floated.user.icon " Characters"))
                       (ui-dropdown-item {:onClick (fn [] (uir/route-to! this `SpellList {}))}
                         (dom/i :.compact.ui.left.floated.magic.icon " Spells"))))
                   (ui-dropdown {:className "item" :text "IP Geolocation"}
                     (ui-dropdown-menu {}
                       (ui-dropdown-item {:onClick (fn [] (uir/route-to! this `IpLookupWidget {}))}
                         (dom/i :.compact.ui.left.floated.search.icon " Lookup IP"))
                       (ui-dropdown-item {:onClick (fn [] (uir/route-to! this `IpLookupList {}))}
                         (dom/i :.compact.ui.left.floated.list.icon " Lookup History"))))
                   (ui-dropdown {:className "item" :text "Weather"}
                     (ui-dropdown-menu {}
                       (ui-dropdown-item {:onClick (fn [] (uir/route-to! this `WeatherLookupWidget {}))}
                         (dom/i :.compact.ui.left.floated.cloud.icon " Get Forecast"))))
                   (dom/div :.ui.right.menu
                     (dom/a :.ui.item {:onClick (fn [] (game/start-game!))}
                       (dom/i :.gamepad.icon) "Whack-a-Toast!")
                     (dom/a :.ui.item {:onClick (fn [] (game/start-ttt!))}
                       (dom/i :.th.icon) "Tic-Tac-Toast!")
                     (dom/div :.ui.item
                       (ui-search search)))))
               ;; dynamic routing
               ;; (div :.ui.segment (ui-main-router router))
               (dom/div :.ui.active.dimmer
                 (dom/div :.ui.large.text.loader "Loading...")))
           (dom/div :.ui.container.segment
             (uir/ui-current-subroute this comp/factory)
             #_(ui-main-router router)
             ))
         ))))

(def ui-root (comp/factory Root))

(comment
  (scf/statechart-session-ident uir/session-id)
  (comp/get-query LandingPage)
  (comp/get-query PersonForm)
  (toast/toast! "Testing"))
