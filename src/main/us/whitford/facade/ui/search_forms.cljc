(ns us.whitford.facade.ui.search-forms
  "Search form ui components"
  (:require
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom]
       :cljs [com.fulcrologic.fulcro.dom :as dom])
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom.events :as events]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.fulcro.mutations :refer [defmutation set-string! set-string!!]]
    [com.fulcrologic.fulcro.raw.components :as rc]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.report-options :as ro]
    [com.fulcrologic.statecharts.integration.fulcro.rad-integration :as ri]
    [taoensso.timbre :as log]
    [us.whitford.facade.model-rad.swapi :as rs]
    [us.whitford.facade.model.swapi :as swapi]
    [us.whitford.facade.ui.swapi-forms :refer [PersonForm FilmForm SpeciesForm PlanetForm VehicleForm StarshipForm]]
    [us.whitford.facade.ui.toast :refer [toast!]]))

(defsc Search [this {:ui/keys [search] :as props}]
  {:query [:ui/search]
   :ident (fn [] [:component/id ::Search])
   :initial-state {:ui/search ""}}
  #_(tap> {:from ::Search :search (string? search)})
  (dom/div :.ui.form
    (dom/div :.ui.action.icon.input
      #_(label "Search terms")
      (dom/input {:type "text"
                  :value search
                  ; :defaultValue ""
                  :placeholder "Search..."
                  :onChange (fn [evt] #?(:clj evt
                                         :cljs (set-string!! this :ui/search :event evt)))})

      (dom/i :.circular.search.link.icon
        {:onClick (fn [evt]
                    (tap> {:from :search/button :this this
                           :evt-target-value (events/target-value evt)
                           :props (comp/props this)
                           :component-name (comp/component-name this)
                           :app (comp/any->app this)
                           :search search})
                    #_(toast! "Hello!")
                    #_(comp/transact! this [`(swapi/search {:search (:ui/search props)})]))}))))

(def ui-search (comp/factory Search))

(comment
  (let [p "person-1"]
    (re-matches #"^.*-(\d+)$" p)))

(report/defsc-report SearchReport [this {:ui/keys [current-rows current-page page-count #_search] :as props}]
  {ro/title "Search Results"
   ro/route "search"
   #_#_ro/query-inclusions [:ui/search]
   ro/source-attribute    :swapi/all-entities
   ro/row-pk              rs/entity_id
   ro/columns             [rs/entity_id rs/entity_name rs/entity_type]
   ro/column-formatters   {:entity/name (fn [this v {:entity/keys [id name type] :as row-props} attr]
                                          (let [id-map {:person   PersonForm
                                                        :film     FilmForm
                                                        :vehicle  VehicleForm
                                                        :starship StarshipForm
                                                        :specie   SpeciesForm
                                                        :planet   PlanetForm}
                                                matches (re-matches #"^.*-(\d+)$" id)
                                                edit-id (last matches)]
                                            (tap> {:from ::SearchReport-column-formatters :id id :name name :type type :id-map id-map :row-props row-props :attr attr :matches matches :edit-id edit-id})
                                            (dom/a {:onClick (fn [] (if edit-id
                                                                        (ri/edit! (comp/any->app this) (id-map type) edit-id)
                                                                        nil))}
                                              (str name))))}
   ro/row-visible? (fn [_ _] true)
   #_(fn [{::keys [filter-content]} {:entity/keys [name]}]
       (let [nm (some-> name (str/lower-case))
             target (some-> filter-content
                            (str/trim)
                            (str/lower-case))]
         (or
           (nil? target)
           (empty? target)
           (and nm (str/includes? nm target)))))
   ro/run-on-mount?       true
   ro/initial-sort-params {:sort-by :entity/name
                           :ascending? true
                           :sortable-columns #{:entity/name :entity/type}}
   ro/controls            {::search! {:type :button
                                      :local? true
                                      :label " Filter "
                                      :class " ui basic compact mini red button "
                                      :action (fn [this _]
                                                (report/filter-rows! this)
                                                #_(control/run! this))}
                           ::search-content {:type :string
                                             :local? true
                                             :placeholder " Search... "
                                             :onChange (fn [this _]
                                                         (report/filter-rows! this)
                                                         #_(control/run! this))}}
   ro/control-layout      {:action-buttons []
                           :inputs         [[::search-content ::search! :_]]}
   ro/before-load (fn [env]
                    (tap> {:from ::SearchReport-before-load :env env})
                    env)
   })

(def ui-searchreport (comp/factory SearchReport))
