(ns us.whitford.facade.ui.swapi-forms
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
    [com.fulcrologic.rad.ids :refer [new-uuid select-keys-in-ns]]
    [com.fulcrologic.rad.picker-options :as po]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.report-options :as ro]
    [com.fulcrologic.rad.semantic-ui-options :as suo]
    [com.fulcrologic.statecharts.integration.fulcro.operations :as sifo]
    [com.fulcrologic.statecharts.integration.fulcro.rad-integration :as ri]
    [com.fulcrologic.statecharts.integration.fulcro.ui-routes :as uir]
    [taoensso.timbre :as log]
    [us.whitford.facade.components.utils :refer [str->int]]
    [us.whitford.facade.model-rad.swapi :as rs]
    [us.whitford.facade.model.swapi :as m.swapi]
    [us.whitford.facade.ui.file-forms :refer [FileForm]])
  #?(:cljs (:require-macros [us.whitford.facade.ui.swapi-forms])))

;; Helper functions for server-side pagination
(defn calculate-page-count
  "Calculate total pages from total count and page size"
  [total-count page-size]
  (if (and total-count page-size (pos? page-size) (pos? total-count))
    (int (Math/ceil (/ total-count page-size)))
    1))

(defn get-current-page-param
  "Get the current page number from report parameters"
  [this]
  (or (control/current-value this ::page) 1))

(defn get-total-count-from-state
  "Get total count from root app state using the pagination metadata key"
  [this total-count-key]
  (let [state (comp/component->state-map this)]
    (get state total-count-key 0)))

(defn pagination-controls
  "Create standard pagination controls for a SWAPI report"
  [total-count-key]
  {::page {:type :int
           :local? true
           :default-value 1}
   ::prior-page {:type :button
                 :label "← Prior"
                 :disabled? (fn [this] (<= (get-current-page-param this) 1))
                 :action (fn [this]
                           (let [current-page (get-current-page-param this)
                                 new-page (max 1 (dec current-page))]
                             (control/set-parameter! this ::page new-page)
                             (control/run! this {:page new-page})))}
   ::next-page {:type :button
                :label "Next →"
                :disabled? (fn [this]
                             (let [current-page (get-current-page-param this)
                                   total-count (get-total-count-from-state this total-count-key)
                                   page-count (calculate-page-count total-count 10)]
                               (>= current-page page-count)))
                :action (fn [this]
                          (let [current-page (get-current-page-param this)
                                new-page (inc current-page)]
                            (control/set-parameter! this ::page new-page)
                            (control/run! this {:page new-page})))}
   ::page-info {:type :button
                :label (fn [this]
                         (let [current-page (get-current-page-param this)
                               total-count (get-total-count-from-state this total-count-key)
                               page-count (calculate-page-count total-count 10)]
                           (str "Page " current-page " of " page-count " (" total-count " total)")))
                :disabled? (constantly true)}})

(defsc PersonQuery [_ _]
  {:query [:person/id :person/name]
   :ident :person/id})

(defsc FilmQuery [_ _]
  {:query [:film/id :film/title]
   :ident :film/id})

(defsc PlanetQuery [_ _]
  {:query [:planet/id :planet/name]
   :ident :planet/id})

(defsc SpeciesQuery [_ _]
  {:query [:specie/id :specie/name]
   :ident :specie/id})

(defsc VehicleQuery [_ _]
  {:query [:vehicle/id :vehicle/name]
   :ident :vehicle/id})

(defsc StarshipQuery [_ _]
  {:query [:starship/id :starship/name]
   :ident :starship/id})

(form/defsc-form PersonForm [this {:person/keys [id name mass eye_color birth_year films
                                                 gender hair_color height homeworld skin_color] :as props}]
  {fo/id             rs/person_id
   fo/title          "Person Details"
   fo/route-prefix   "person"
   #_#_fo/default-values {}
   fo/attributes     [rs/person_id rs/person_name rs/person_birth_year rs/person_eye_color
                      rs/person_films rs/person_gender rs/person_hair_color rs/person_height
                      rs/person_homeworld rs/person_mass rs/person_skin_color]

   fo/field-styles {:person/films :pick-many
                    :person/homeworld :pick-one}
   fo/field-options {:person/films {po/query-key :swapi/all-films
                                    #_#_po/query-component FilmQuery
                                    po/query [:film/id :film/title]
                                    po/form ::FilmForm
                                    po/allow-create? false
                                    po/allow-edit? false
                                    po/cache-time-ms 30000
                                    #_#_po/cache-key :all-films-options
                                    po/options-xform (fn [_ options]
                                                       (tap> {:from ::field-options-person-films :options options})
                                                       (mapv (fn [{:film/keys [id title]}]
                                                               #_(tap> {:from ::field-options-person-films :id id :title title})
                                                               {:text (str title) :value [:film/id id]})
                                                         (sort-by :film/id options)))}
                     :person/homeworld {:style :dropdown
                                        po/query-key :swapi/all-planets
                                        po/query-component PlanetQuery
                                        po/allow-create? false
                                        po/allow-edit? false
                                        po/cache-time-ms 30000
                                        po/cache-key :all-planets-options
                                        po/options-xform (fn [_ options]
                                                           (tap> {:from ::field-options-person-homeworld :options options})
                                                           (mapv (fn [{:planet/keys [id name]}]
                                                                   #_(tap> {:from ::field-options-person-homeworld :id id :name name})
                                                                   {:text (str name) :value [:planet/id id]})
                                                             (sort-by :planet/name options)))}}
   fo/cancel-route   ::PersonList
   fo/debug?         true
   fo/read-only?     true
   fo/silent-abandon? true})

(def ui-person-form (comp/factory PersonForm))

(defsc Person [this props]
  {:query [:person/id :person/name]
   :initial-state {}})

#_(defsc Person [this {:person/keys [id name birth_year eye_color gender hair_color
                                     height mass skin_color] :as props}]
    {:query [[{:swapi/person [:person/id :person/name :person/eye_color :person/birth_year
                              :person/mass :person/gender :person/hair_color :person/height
                              :person/skin_color]}]]
     :ident :person/id
     :initial-state {}}
    (tap> {:from ::Person :props props})
    (dom/div :.ui.form
      (dom/input {:type "text"
                  :value id})))

(def ui-person (comp/factory Person))

(comment
  (comp/component-options PersonForm))

(defsc PersonListItem [this
                       {:person/keys [id name mass birth_year eye_color films
                                      hair_color gender height skin_color homeworld] :as props}
                       {:keys [report-instance row-class ::report/idx]}]
  {:query [:person/id :person/name :person/mass :person/eye_color :person/birth_year :person/films
           :person/hair_color :person/gender :person/height :person/skin_color :person/homeworld]
   :ident :person/id}
  (let [{:keys [edit-form entity-id]} (report/form-link report-instance props :person/id)]
    (tap> {:from ::PersonListItem :edit-form edit-form :entity-id entity-id})
    (dom/div :.item
      (dom/div :.content
        (if edit-form
            (dom/a :.link.header {:onClick (fn [] (ri/edit! this edit-form entity-id))} name)
            (dom/div :.header name))))))

(def ui-person-list-item (comp/factory PersonListItem))

(comment
  (let [p {:person/id 1 :person/name "test" :entity/id 1}]
    (select-keys-in-ns p "person"))
  )

(report/defsc-report PersonList [this {:ui/keys [current-rows current-page page-count] :as props}]
  {ro/title "All People"
   ro/route "people"
   ro/source-attribute    :swapi/all-people
   ro/row-pk              rs/person_id
   ro/columns             [rs/person_name rs/person_birth_year rs/person_eye_color rs/person_mass]
   ro/query-inclusions    [:swapi.people/total-count :swapi.people/current-page :swapi.people/page-size]
   ro/column-formatters   {:person/name (fn [this v {:person/keys [id] :as p}]
                                          (dom/a {:onClick #(ri/edit! this PersonForm id)} (str v)))}
   ro/row-visible? (fn [{::keys [filter-content]} {:person/keys [name]}]
                     (let [nm (some-> name (str/lower-case))
                           target (some-> filter-content
                                          (str/trim)
                                          (str/lower-case))]
                       (or
                         (nil? target)
                         (empty? target)
                         (and nm (str/includes? nm target)))))
   ro/run-on-mount?       true
   ro/initial-sort-params {:sort-by :person/id
                           :ascending? true
                           :sortable-columns #{:person/name :person/birth_year :person/mass :person/eye_color}}
   ro/controls            (merge
                            (pagination-controls :swapi.people/total-count)
                            {::refresh {:type :button
                                        :label "Refresh"
                                        :action (fn [this]
                                                  (let [current-page (get-current-page-param this)]
                                                    (control/run! this {:page current-page})))}
                             ::search! {:type :button
                                        :local? true
                                        :label " Filter "
                                        :class "ui red basic tiny button"
                                        :action (fn [this _] (report/filter-rows! this))}
                             ::filter-content {:type :string
                                               :local? true
                                               :placeholder " Type a partial person "
                                               :onChange (fn [this _] (report/filter-rows! this))}})
   ro/control-layout      {:action-buttons [::refresh ::prior-page ::page-info ::next-page]
                           :inputs         [[::filter-content ::search!]
                                            [:_]]}
   suo/rendering-options {#_#_suo/report-table-class "ui very compact selectable table"
                          #_#_suo/report-table-header-class (fn [this i] (case i
                                                                           0 ""
                                                                           1 "center aligned"
                                                                           "collapsing"))
                          #_#_suo/report-table-cell-class (fn [this i] (case i
                                                                         0 ""
                                                                         1 "center aligned"
                                                                         "collapsing"))}
   })

(def ui-person-list (comp/factory PersonList))

(form/defsc-form FilmForm [this {:film/keys [id title] :as props}]
  {fo/id             rs/film_id
   fo/title          "Film Details"
   fo/route-prefix   "film"
   fo/default-values {}
   fo/attributes     [rs/film_title rs/film_release_date rs/film_director
                      rs/film_episode_id]
   fo/cancel-route   ::FilmList
   fo/debug?         true
   fo/read-only?     true
   fo/silent-abandon? true})

(def ui-film-form (comp/factory FilmForm))

(report/defsc-report FilmList [this props]
  {ro/title "All Films"
   ro/route "films"
   ro/source-attribute    :swapi/all-films
   ro/row-pk              rs/film_id
   ro/columns             [rs/film_title rs/film_director rs/film_release_date]
   ro/query-inclusions    [:swapi.films/total-count :swapi.films/current-page :swapi.films/page-size]
   ro/column-formatters   {:film/title (fn [this _ {:film/keys [id title] :as params}]
                                         (dom/a {:onClick (fn [] (ri/edit! this FilmForm id))}
                                           (str title)))}
   ro/row-visible? (fn [{::keys [filter-content]} {:film/keys [title]}]
                     (let [nm (some-> title (str/lower-case))
                           target (some-> filter-content
                                          (str/trim)
                                          (str/lower-case))]
                       (or
                         (nil? target)
                         (empty? target)
                         (and nm (str/includes? nm target)))))
   ro/run-on-mount?       true
   ro/initial-sort-params {:sort-by :film/release_date
                           :ascending? true
                           :sortable-columns #{:film/title}}
   ro/controls            (merge
                            (pagination-controls :swapi.films/total-count)
                            {::refresh {:type :button
                                        :label "Refresh"
                                        :action (fn [this]
                                                  (let [current-page (get-current-page-param this)]
                                                    (control/run! this {:page current-page})))}
                             ::search! {:type :button
                                        :local? true
                                        :label " Filter "
                                        :class " ui basic compact mini red button "
                                        :action (fn [this _] (report/filter-rows! this))}
                             ::filter-content {:type :string
                                               :local? true
                                               :placeholder " Type a partial film "
                                               :onChange (fn [this _] (report/filter-rows! this))}})
   ro/control-layout      {:action-buttons [::refresh ::prior-page ::page-info ::next-page]
                           :inputs         [[::filter-content ::search! :_]]}
   })

(def ui-film-list (comp/factory FilmList))

(form/defsc-form PlanetForm [this {:planet/keys [id name] :as props}]
  {fo/id             rs/planet_id
   fo/title          "Planet Details"
   fo/route-prefix   "planet"
   fo/default-values {}
   fo/attributes     [rs/planet_name rs/planet_climate rs/planet_terrain
                      rs/planet_gravity rs/planet_diameter rs/planet_orbital_period
                      rs/planet_population rs/planet_rotation_period]
   fo/cancel-route   ::PlanetList
   fo/debug?         true
   fo/read-only?     true
   fo/silent-abandon? true})

(def ui-planet-form (comp/factory PlanetForm))

(report/defsc-report PlanetList [this props]
  {ro/title "All Planets"
   ro/route "planets"
   ro/source-attribute    :swapi/all-planets
   ro/row-pk              rs/planet_id
   ro/columns             [rs/planet_name rs/planet_climate rs/planet_terrain]
   ro/query-inclusions    [:swapi.planets/total-count :swapi.planets/current-page :swapi.planets/page-size]
   ro/column-formatters   {:planet/name (fn [this _ {:planet/keys [id name] :as params}]
                                          (dom/a {:onClick (fn [] (ri/edit! this PlanetForm id))}
                                            (str name)))}
   ro/row-visible? (fn [{::keys [filter-content]} {:planet/keys [name]}]
                     (let [nm (some-> name (str/lower-case))
                           target (some-> filter-content
                                          (str/trim)
                                          (str/lower-case))]
                       (or
                         (nil? target)
                         (empty? target)
                         (and nm (str/includes? nm target)))))
   ro/run-on-mount?       true
   ro/initial-sort-params {:sort-by :planet/name
                           :ascending? true
                           :sortable-columns #{:planet/name}}
   ro/controls            (merge
                            (pagination-controls :swapi.planets/total-count)
                            {::refresh {:type :button
                                        :label "Refresh"
                                        :action (fn [this]
                                                  (let [current-page (get-current-page-param this)]
                                                    (control/run! this {:page current-page})))}
                             ::search! {:type :button
                                        :local? true
                                        :label " Filter "
                                        :class " ui basic compact mini red button "
                                        :action (fn [this _] (report/filter-rows! this))}
                             ::filter-content {:type :string
                                               :local? true
                                               :placeholder " Type a partial planet "
                                               :onChange (fn [this _] (report/filter-rows! this))}})
   ro/control-layout      {:action-buttons [::refresh ::prior-page ::page-info ::next-page]
                           :inputs         [[::filter-content ::search! :_]]}
   })

(def ui-planet-list (comp/factory PlanetList))

(form/defsc-form SpeciesForm [this {:specie/keys [id name] :as props}]
  {fo/id             rs/species_id
   fo/title          "Species Details"
   fo/route-prefix   "specie"
   fo/default-values {}
   fo/attributes     [rs/species_name rs/species_classification rs/species_designation
                      rs/species_eye_colors rs/species_hair_colors rs/species_language
                      rs/species_average_height rs/species_average_lifespan]
   fo/cancel-route   ::SpeciesList
   fo/debug?         true
   fo/read-only?     true
   fo/silent-abandon? true})

(def ui-species-form (comp/factory SpeciesForm))

(report/defsc-report SpeciesList [this props]
  {ro/title "All Species"
   ro/route "species"
   ro/source-attribute    :swapi/all-species
   ro/row-pk              rs/species_id
   ro/columns             [rs/species_name rs/species_classification rs/species_designation
                           rs/species_language]
   ro/query-inclusions    [:swapi.species/total-count :swapi.species/current-page :swapi.species/page-size]
   ro/column-formatters   {:specie/name (fn [this _ {:specie/keys [id name] :as params}]
                                          (dom/a {:onClick (fn [] (ri/edit! this SpeciesForm id))}
                                            (str name)))}
   ro/row-visible? (fn [{::keys [filter-content]} {:specie/keys [name]}]
                     (let [nm (some-> name (str/lower-case))
                           target (some-> filter-content
                                          (str/trim)
                                          (str/lower-case))]
                       (or
                         (nil? target)
                         (empty? target)
                         (and nm (str/includes? nm target)))))
   ro/run-on-mount?       true
   ro/initial-sort-params {:sort-by :specie/name
                           :ascending? true
                           :sortable-columns #{:specie/name :specie/classification :specie/designation}}
   ro/controls            (merge
                            (pagination-controls :swapi.species/total-count)
                            {::refresh {:type :button
                                        :label "Refresh"
                                        :action (fn [this]
                                                  (let [current-page (get-current-page-param this)]
                                                    (control/run! this {:page current-page})))}
                             ::search! {:type :button
                                        :local? true
                                        :label " Filter "
                                        :class " ui basic compact mini red button "
                                        :action (fn [this _] (report/filter-rows! this))}
                             ::filter-content {:type :string
                                               :local? true
                                               :placeholder " Type a partial species "
                                               :onChange (fn [this _] (report/filter-rows! this))}})
   ro/control-layout      {:action-buttons [::refresh ::prior-page ::page-info ::next-page]
                           :inputs         [[::filter-content ::search! :_]]}
   })

(def ui-species-list (comp/factory SpeciesList))

(form/defsc-form VehicleForm [this {:vehicle/keys [id name] :as props}]
  {fo/id             rs/vehicle_id
   fo/title          "Vehicle Details"
   fo/route-prefix   "vehicle"
   fo/default-values {}
   fo/attributes     [rs/vehicle_name rs/vehicle_capacity rs/vehicle_consumables rs/vehicle_cost_in_credits
                      rs/vehicle_crew rs/vehicle_films rs/vehicle_model rs/vehicle_manufacturer
                      rs/vehicle_passengers rs/vehicle_pilots]
   fo/cancel-route   ::VehicleList
   fo/debug?         true
   fo/read-only?     true
   fo/silent-abandon? true})

(def ui-vehicle-form (comp/factory VehicleForm))

(report/defsc-report VehicleList [this props]
  {ro/title "All Vehicles"
   ro/route "vehicles"
   ro/source-attribute    :swapi/all-vehicles
   ro/row-pk              rs/vehicle_id
   ro/columns             [rs/vehicle_name rs/vehicle_model rs/vehicle_manufacturer]
   ro/query-inclusions    [:swapi.vehicles/total-count :swapi.vehicles/current-page :swapi.vehicles/page-size]
   ro/column-formatters   {:vehicle/name (fn [this _ {:vehicle/keys [id name] :as params}]
                                           (dom/a {:onClick (fn [] (ri/edit! this VehicleForm id))}
                                             (str name)))}
   ro/row-visible? (fn [{::keys [filter-content]} {:vehicle/keys [name]}]
                     (let [nm (some-> name (str/lower-case))
                           target (some-> filter-content
                                          (str/trim)
                                          (str/lower-case))]
                       (or
                         (nil? target)
                         (empty? target)
                         (and nm (str/includes? nm target)))))
   ro/run-on-mount?       true
   ro/initial-sort-params {:sort-by :vehicle/name
                           :ascending? true
                           :sortable-columns #{:vehicle/name :vehicle/model :vehicle/manufacturer}}
   ro/controls            (merge
                            (pagination-controls :swapi.vehicles/total-count)
                            {::refresh {:type :button
                                        :label "Refresh"
                                        :action (fn [this]
                                                  (let [current-page (get-current-page-param this)]
                                                    (control/run! this {:page current-page})))}
                             ::search! {:type :button
                                        :local? true
                                        :label " Filter "
                                        :class " ui basic compact mini red button "
                                        :action (fn [this _] (report/filter-rows! this))}
                             ::filter-content {:type :string
                                               :local? true
                                               :placeholder " Type a partial vehicle "
                                               :onChange (fn [this _] (report/filter-rows! this))}})
   ro/control-layout      {:action-buttons [::refresh ::prior-page ::page-info ::next-page]
                           :inputs         [[::filter-content ::search! :_]]}
   })

(def ui-vehicle-list (comp/factory VehicleList))

(form/defsc-form StarshipForm [this {:starship/keys [id name] :as props}]
  {fo/id             rs/starship_id
   fo/title          "Star Ship Details"
   fo/route-prefix   "starship"
   fo/default-values {}
   fo/attributes     [rs/starship_name rs/starship_cargo_capacity rs/starship_consumables
                      rs/starship_cost_in_credits rs/starship_crew rs/starship_films rs/starship_hyperdrive_rating
                      rs/starship_length rs/starship_manufacturer rs/starship_max_atmosphering_speed rs/starship_model
                      rs/starship_passengers rs/starship_pilots rs/starship_class]
   fo/cancel-route   ::StarshipList
   fo/debug?         true
   fo/read-only?     true
   fo/silent-abandon? true})

(def ui-starship-form (comp/factory StarshipForm))

(report/defsc-report StarshipList [this props]
  {ro/title "All Star Ships"
   ro/route "starships"
   ro/source-attribute    :swapi/all-starships
   ro/row-pk              rs/starship_id
   ro/columns             [rs/starship_name rs/starship_model rs/starship_manufacturer]
   ro/query-inclusions    [:swapi.starships/total-count :swapi.starships/current-page :swapi.starships/page-size]
   ro/column-formatters   {:starship/name (fn [this _ {:starship/keys [id name] :as params}]
                                            (dom/a {:onClick (fn [] (ri/edit! this StarshipForm id))}
                                              (str name)))}
   ro/row-visible? (fn [{::keys [filter-content]} {:starship/keys [name]}]
                     (let [nm (some-> name (str/lower-case))
                           target (some-> filter-content
                                          (str/trim)
                                          (str/lower-case))]
                       (or
                         (nil? target)
                         (empty? target)
                         (and nm (str/includes? nm target)))))
   ro/run-on-mount?       true
   ro/initial-sort-params {:sort-by :starship/name
                           :ascending? true
                           :sortable-columns #{:starship/name :starship/model :starship/manufacturer}}
   ro/controls            (merge
                            (pagination-controls :swapi.starships/total-count)
                            {::refresh {:type :button
                                        :label "Refresh"
                                        :action (fn [this]
                                                  (let [current-page (get-current-page-param this)]
                                                    (control/run! this {:page current-page})))}
                             ::search! {:type :button
                                        :local? true
                                        :label " Filter "
                                        :class " ui basic compact mini red button "
                                        :action (fn [this _] (report/filter-rows! this))}
                             ::filter-content {:type :string
                                               :local? true
                                               :placeholder " Type a partial starship "
                                               :onChange (fn [this _] (report/filter-rows! this))}})
   ro/control-layout      {:action-buttons [::refresh ::prior-page ::page-info ::next-page]
                           :inputs         [[::filter-content ::search! :_]]}
   })

(def ui-starship-list (comp/factory StarshipList))
