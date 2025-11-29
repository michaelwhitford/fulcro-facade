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
    [com.fulcrologic.statecharts.integration.fulcro.ui-routes :as uir]
    [taoensso.timbre :as log]
    [us.whitford.facade.model-rad.swapi :as rs]
    [us.whitford.facade.model.swapi :as swapi]
    [us.whitford.facade.ui.swapi-forms :refer [PersonForm FilmForm SpeciesForm PlanetForm VehicleForm StarshipForm]]
    [us.whitford.facade.ui.toast :refer [toast!]]))

;; Forward declare SearchReport for use in set-search-term mutation
(declare SearchReport)

(defmutation set-search-term
  "Set the search term in the SearchReport control parameters"
  [{:keys [search-term]}]
  (action [{:keys [state]}]
    (let [report-ident (comp/get-ident SearchReport {})]
      (swap! state assoc-in (conj report-ident :ui/parameters ::search-term) search-term))))

(defsc Search [this {:ui/keys [search] :as props}]
  {:query [:ui/search]
   :ident (fn [] [:component/id ::Search])
   :initial-state {:ui/search ""}}
  (let [do-search (fn []
                    (when (and search (not (str/blank? search)))
                      (let [app (comp/any->app this)
                            search-term (str/trim search)]
                        ;; Set the search term in the report's parameters
                        (comp/transact! app [(set-search-term {:search-term search-term})])
                        ;; Navigate to SearchReport and trigger reload with search term
                        (uir/route-to! app `SearchReport {:query-params {:search search-term}})
                        ;; Clear the search input after navigation
                        (set-string!! this :ui/search :value ""))))]
    (dom/div :.ui.form
      (dom/div :.ui.action.icon.input
        (dom/input {:type "text"
                    :value (or search "")
                    :placeholder "Search SWAPI..."
                    :onChange (fn [evt] #?(:clj evt
                                           :cljs (set-string!! this :ui/search :event evt)))
                    :onKeyDown (fn [evt]
                                 #?(:cljs
                                    (when (= "Enter" (.-key evt))
                                      (do-search))))})
        (dom/i :.circular.search.link.icon
          {:onClick (fn [_evt] (do-search))})))))

(def ui-search (comp/factory Search))

(comment
  (let [p "person-1"]
    (re-matches #"^.*-(\d+)$" p)))

(defn get-search-term
  "Get the current search term from report parameters"
  [report-instance]
  (let [params (report/current-control-parameters report-instance)]
    (or (::search-term params) "")))

(defn entity-type-icon
  "Return the appropriate icon class for an entity type"
  [entity-type]
  (case entity-type
    :person "user"
    :film "film"
    :vehicle "car"
    :starship "space shuttle"
    :specie "hand spock"
    :planet "globe"
    "question"))

(defsc SearchResultRow [this props]
  {:query [:entity/id :entity/name :entity/type]
   :ident (fn [] [:entity/id (:entity/id props)])}
  (let [{:entity/keys [id name type]} props
        id-map {:person   PersonForm
                :film     FilmForm
                :vehicle  VehicleForm
                :starship StarshipForm
                :specie   SpeciesForm
                :planet   PlanetForm}
        matches (re-matches #"^.*-(\d+)$" id)
        edit-id (last matches)]
    (dom/div :.ui.segment {:style {:marginBottom "10px"}}
      (dom/div :.ui.header
        (dom/i {:className (str "ui " (entity-type-icon type) " icon")})
        (dom/a {:style {:cursor "pointer"}
                :onClick (fn [] (when edit-id
                                  (ri/edit! (comp/any->app this) (id-map type) edit-id)))}
          name))
      (dom/div :.ui.label (str/capitalize (name type))))))

(def ui-search-result-row (comp/factory SearchResultRow {:keyfn :entity/id}))

(report/defsc-report SearchReport [this props]
  {ro/title "SWAPI Cross-Entity Search"
   ro/route "search"
   ro/source-attribute    :swapi/all-entities
   ro/row-pk              rs/entity_id
   ro/columns             [rs/entity_type rs/entity_name rs/entity_id]
   ro/column-headings     {:entity/type "Type"
                           :entity/name "Name"
                           :entity/id "ID"}
   ro/column-formatters   {:entity/type (fn [_this v _row-props _attr]
                                          (dom/span
                                            (dom/i {:className (str "ui " (entity-type-icon v) " icon")})
                                            (str/capitalize (name v))))
                           :entity/name (fn [this v {:entity/keys [id type] :as _row-props} _attr]
                                          (let [id-map {:person   PersonForm
                                                        :film     FilmForm
                                                        :vehicle  VehicleForm
                                                        :starship StarshipForm
                                                        :specie   SpeciesForm
                                                        :planet   PlanetForm}
                                                matches (re-matches #"^.*-(\d+)$" id)
                                                edit-id (last matches)]
                                            (dom/a {:style {:cursor "pointer" :color "#4183c4"}
                                                    :onClick (fn [] (when edit-id
                                                                      (ri/edit! (comp/any->app this) (id-map type) edit-id)))}
                                              (str v))))
                           :entity/id (fn [_this v _row-props _attr]
                                        (dom/span :.ui.small.grey.text (str v)))}
   ro/row-visible? (fn [{::keys [filter-term]} {:entity/keys [name type]}]
                     (let [nm (some-> name str/lower-case)
                           typ (some-> type name str/lower-case)
                           target (some-> filter-term str/trim str/lower-case)]
                       (or
                         (nil? target)
                         (empty? target)
                         (and nm (str/includes? nm target))
                         (and typ (str/includes? typ target)))))
   ro/run-on-mount?       true
   ro/initial-sort-params {:sort-by :entity/type
                           :ascending? true
                           :sortable-columns #{:entity/name :entity/type}}
   ro/controls            {::search-term {:type :string
                                          :label "Search Term"
                                          :placeholder "Enter search term (e.g., 'luke', 'tatooine', 'x-wing')..."
                                          :style {:minWidth "400px"}}
                           ::search! {:type :button
                                      :label "Search SWAPI"
                                      :class "ui primary button"
                                      :action (fn [this _]
                                                (control/run! this))}
                           ::filter-term {:type :string
                                          :local? true
                                          :placeholder "Filter results..."
                                          :onChange (fn [this _]
                                                      (report/filter-rows! this))}
                           ::clear! {:type :button
                                     :label "Clear"
                                     :class "ui basic button"
                                     :action (fn [this _]
                                               (control/set-parameter! this ::search-term "")
                                               (control/set-parameter! this ::filter-term ""))}
                           ::result-count {:type :button
                                           :label (fn [this]
                                                    (let [rows (report/current-rows this)]
                                                      (str (count rows) " results found")))
                                           :disabled? (constantly true)
                                           :class "ui basic label"}}
   ro/control-layout      {:action-buttons [::search! ::clear! ::result-count]
                           :inputs         [[::search-term]
                                            [::filter-term]]}
   ro/before-load (fn [env]
                    (let [search-term (get-in env [:params ::search-term])]
                      (log/info "SearchReport before-load" {:search-term search-term})
                      (assoc-in env [:query-params :search] (or search-term ""))))
   ro/row-actions [{:label "View"
                    :action (fn [this {:entity/keys [id type]}]
                              (let [id-map {:person   PersonForm
                                            :film     FilmForm
                                            :vehicle  VehicleForm
                                            :starship StarshipForm
                                            :specie   SpeciesForm
                                            :planet   PlanetForm}
                                    matches (re-matches #"^.*-(\d+)$" id)
                                    edit-id (last matches)]
                                (when edit-id
                                  (ri/edit! (comp/any->app this) (id-map type) edit-id))))}]
   ro/BodyItem SearchResultRow
   })

(def ui-searchreport (comp/factory SearchReport))
