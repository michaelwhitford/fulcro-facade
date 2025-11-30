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
    [us.whitford.facade.model-rad.entity :as re]
    [us.whitford.facade.ui.hpapi-forms :refer [CharacterForm SpellForm]]
    [us.whitford.facade.ui.swapi-forms :refer [PersonForm FilmForm SpeciesForm PlanetForm VehicleForm StarshipForm]]
    [us.whitford.facade.ui.toast :refer [toast!]]))

;; Forward declare SearchReport for use in set-search-term mutation
(declare SearchReport)

(defmutation set-search-term-and-run
  "Set the search term and trigger report run in a single transaction.
   Uses the global control path since ::search-term is not a local control."
  [{:keys [search-term]}]
  (action [{:keys [state app] :as env}]
    ;; Non-local controls are stored at [::control/id <key> ::control/value]
    (log/info "set-search-term-and-run mutation" {:search-term search-term :app-exists? (boolean app)})
    (swap! state assoc-in [::control/id ::search-term ::control/value] search-term)
    ;; Trigger the report to run after state is updated
    #?(:cljs (when app
               (js/setTimeout 
                 (fn []
                   (log/info "Running report with search term" {:search-term search-term})
                   (report/run-report! app SearchReport))
                 100)))))

(defsc Search [this {:ui/keys [search] :as props}]
  {:query [:ui/search]
   :ident (fn [] [:component/id ::Search])
   :initial-state {:ui/search ""}}
  (let [do-search (fn []
                    (when (and search (not (str/blank? search)))
                      (let [app (comp/any->app this)
                            search-term (str/trim search)]
                        (log/info "Search submitted" {:search-term search-term})
                        ;; Navigate to SearchReport first
                        (uir/route-to! app `SearchReport {})
                        ;; Then set the search term and run the report in one transaction
                        (comp/transact! app [(set-search-term-and-run {:search-term search-term})])
                        ;; Clear the search input after navigation
                        (set-string!! this :ui/search :value ""))))]
    (dom/div :.ui.form
      (dom/div :.ui.action.icon.input
        (dom/input {:type "text"
                    :value (or search "")
                    :placeholder "Search Star Wars & Harry Potter..."
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
    ;; SWAPI types
    :person "user"
    :film "film"
    :vehicle "car"
    :starship "space shuttle"
    :specie "hand spock"
    :planet "globe"
    ;; Harry Potter types
    :character "magic"
    :spell "bolt"
    "question"))

(def entity-type->form
  "Map of entity types to their detail forms"
  {:person    PersonForm
   :film      FilmForm
   :vehicle   VehicleForm
   :starship  StarshipForm
   :specie    SpeciesForm
   :planet    PlanetForm
   :character CharacterForm
   :spell     SpellForm})

(defn parse-entity-id
  "Parse entity ID string to extract the raw ID portion.
   Handles both numeric IDs (SWAPI: 'person-1') and UUIDs (HP: 'character-abc123-def456')"
  [entity-id]
  (when entity-id
    ;; Match type-id where id can be numeric or UUID
    (when-let [match (re-matches #"^([^-]+)-(.+)$" entity-id)]
      (nth match 2))))

(defsc SearchResultRow [this props]
  {:query [:entity/id :entity/name :entity/type]
   :ident :entity/id}
  (let [{:entity/keys [id name type]} props
        edit-id (parse-entity-id id)
        form-class (get entity-type->form type)]
    (dom/div :.ui.segment {:style {:marginBottom "10px"}}
      (dom/div :.ui.header
        (dom/i {:className (str "ui " (entity-type-icon type) " icon")})
        (dom/a {:style {:cursor "pointer"}
                :onClick (fn [] (when (and edit-id form-class)
                                  (ri/edit! (comp/any->app this) form-class edit-id)))}
          name))
      (dom/div :.ui.label (str/capitalize (clojure.core/name type))))))

(def ui-search-result-row (comp/factory SearchResultRow {:keyfn :entity/id}))

(report/defsc-report SearchReport [this props]
  {ro/title "Universal Search"
   ro/route "search"
   ro/source-attribute    :swapi/all-entities
   ro/row-pk              re/entity_id
   ro/columns             [re/entity_type re/entity_name re/entity_id]
   ro/column-headings     {:entity/type "Type"
                           :entity/name "Name"
                           :entity/id "ID"}
   ro/column-formatters   {:entity/type (fn [_this v _row-props _attr]
                                          (dom/span
                                            (dom/i {:className (str "ui " (entity-type-icon v) " icon")})
                                            (str/capitalize (clojure.core/name v))))
                           :entity/name (fn [this v {:entity/keys [id type] :as _row-props} _attr]
                                          (let [edit-id (parse-entity-id id)
                                                form-class (get entity-type->form type)]
                                            (dom/a {:style {:cursor "pointer" :color "#4183c4"}
                                                    :onClick (fn [] (when (and edit-id form-class)
                                                                      (ri/edit! (comp/any->app this) form-class edit-id)))}
                                              (str v))))
                           :entity/id (fn [_this v _row-props _attr]
                                        (dom/span :.ui.small.grey.text (str v)))}
   ;; Temporarily disabled to debug empty rows issue
   #_ro/row-visible? #_(fn [{::keys [filter-term]} {:entity/keys [name type]}]
                     (let [nm (some-> name str/lower-case)
                           typ (some-> type name str/lower-case)
                           target (some-> filter-term str/trim str/lower-case)]
                       (or
                         (nil? target)
                         (empty? target)
                         (and nm (str/includes? nm target))
                         (and typ (str/includes? typ target)))))
   ro/run-on-mount?       false
   ro/initial-sort-params {:sort-by :entity/type
                           :ascending? true
                           :sortable-columns #{:entity/name :entity/type}}
   ro/controls            {::search-term {:type :string
                                          :label "Search Term"
                                          :placeholder "Search Star Wars & Harry Potter (e.g., 'luke', 'harry', 'levios')..."
                                          :style {:minWidth "400px"}}
                           ::search! {:type :button
                                      :label "Search"
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
   ;; Map the namespaced control key to the resolver's expected :search-term key
   ;; load-options merges with the default load params, and :params key overrides
   ro/load-options (fn [env]
                     (let [params (report/current-control-parameters env)
                           search-term (::search-term params)]
                       (log/info "SearchReport load-options" {:search-term search-term :all-params params})
                       {:params (assoc params :search-term search-term)}))
   ro/row-actions [{:label "View"
                    :action (fn [this {:entity/keys [id type]}]
                              (let [edit-id (parse-entity-id id)
                                    form-class (get entity-type->form type)]
                                (when (and edit-id form-class)
                                  (ri/edit! (comp/any->app this) form-class edit-id))))}]
   ro/BodyItem SearchResultRow
   })

(def ui-searchreport (comp/factory SearchReport))
