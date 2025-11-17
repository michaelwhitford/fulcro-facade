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
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
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
    [us.whitford.facade.ui.search-forms :refer [Search ui-search SearchReport]]
    [us.whitford.facade.ui.swapi-forms :refer [PersonList PersonForm Person
                                               FilmList FilmForm
                                               PlanetList PlanetForm
                                               SpeciesList SpeciesForm
                                               VehicleList VehicleForm
                                               StarshipList StarshipForm]]

    [us.whitford.facade.ui.toast :as toast]))

(defsc LandingPage [this props]
  {:query         ['*]
   :ident         (fn [] [:component/id ::LandingPage])
   :initial-state {}
   :route-segment ["landing-page"]
   :use-hooks? true}
  (dom/div :.ui.header "Welcome to Facade!"))

;; This will just be a normal router...but there can be many of them.
(defrouter MainRouter [this {:keys [current-state route-factory route-props]}]
  {:always-render-body? true
   :router-targets      [LandingPage AccountList AccountForm PersonList PersonForm Person Search]}
  ;; Normal Fulcro code to show a loader on slow route change (assuming Semantic UI here, should
  ;; be generalized for RAD so UI-specific code isn't necessary)
  (dom/div
    (dom/div :.ui.loader {:classes [(when-not (= :routed current-state) "active")]})
    (when route-factory
          (route-factory route-props))))

(def ui-main-router (comp/factory MainRouter))

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
                   (dom/div :.ui.right.menu
                     #_(dom/div :.ui.small.loader {:classes [(when busy? "active")]})
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
