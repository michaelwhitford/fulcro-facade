(ns us.whitford.facade.ui.hpapi-forms
  (:require
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom]
       :cljs [com.fulcrologic.fulcro.dom :as dom])
    [clojure.string :as str]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.report-options :as ro]
    [com.fulcrologic.statecharts.integration.fulcro.rad-integration :as ri]
    [us.whitford.facade.model-rad.hpapi :as rh]))

;; Character Form

(form/defsc-form CharacterForm [this {:character/keys [id name] :as props}]
  {fo/id             rh/character_id
   fo/title          "Character Details"
   fo/route-prefix   "character"
   fo/default-values {}
   fo/attributes     [rh/character_name rh/character_house rh/character_species
                      rh/character_gender rh/character_dateOfBirth rh/character_yearOfBirth
                      rh/character_wizard rh/character_ancestry rh/character_eyeColour
                      rh/character_hairColour rh/character_patronus rh/character_hogwartsStudent
                      rh/character_hogwartsStaff rh/character_actor rh/character_alive]
   fo/cancel-route   ::CharacterList
   fo/debug?         true
   fo/read-only?     true
   fo/silent-abandon? true})

(def ui-character-form (comp/factory CharacterForm))

;; Character List Report

(report/defsc-report CharacterList [this props]
  {ro/title "All Characters"
   ro/route "characters"
   ro/source-attribute    :hpapi/all-characters
   ro/row-pk              rh/character_id
   ro/columns             [rh/character_name rh/character_house rh/character_species
                           rh/character_ancestry]
   ro/column-formatters   {:character/name (fn [this _ {:character/keys [id name] :as params}]
                                             (dom/a {:onClick (fn [] (ri/edit! this CharacterForm id))}
                                               (str name)))}
   ro/row-visible? (fn [{::keys [filter-content]} {:character/keys [name]}]
                     (let [nm (some-> name (str/lower-case))
                           target (some-> filter-content
                                          (str/trim)
                                          (str/lower-case))]
                       (or
                         (nil? target)
                         (empty? target)
                         (and nm (str/includes? nm target)))))
   ro/run-on-mount?       true
   ro/initial-sort-params {:sort-by :character/name
                           :ascending? true
                           :sortable-columns #{:character/name :character/house :character/species}}
   ro/controls            {::search! {:type :button
                                      :local? true
                                      :label " Filter "
                                      :class " ui basic compact mini red button "
                                      :action (fn [this _] (report/filter-rows! this))}
                           ::filter-content {:type :string
                                             :local? true
                                             :placeholder " Type a partial character name "
                                             :onChange (fn [this _] (report/filter-rows! this))}}
   ro/control-layout      {:action-buttons []
                           :inputs         [[::filter-content ::search! :_]]}
   })

(def ui-character-list (comp/factory CharacterList))

;; Spell Form

(form/defsc-form SpellForm [this {:spell/keys [id name] :as props}]
  {fo/id             rh/spell_id
   fo/title          "Spell Details"
   fo/route-prefix   "spell"
   fo/default-values {}
   fo/attributes     [rh/spell_name rh/spell_description]
   fo/cancel-route   ::SpellList
   fo/debug?         true
   fo/read-only?     true
   fo/silent-abandon? true})

(def ui-spell-form (comp/factory SpellForm))

;; Spell List Report

(report/defsc-report SpellList [this props]
  {ro/title "All Spells"
   ro/route "spells"
   ro/source-attribute    :hpapi/all-spells
   ro/row-pk              rh/spell_id
   ro/columns             [rh/spell_name rh/spell_description]
   ro/column-formatters   {:spell/name (fn [this _ {:spell/keys [id name] :as params}]
                                         (dom/a {:onClick (fn [] (ri/edit! this SpellForm id))}
                                           (str name)))}
   ro/row-visible? (fn [{::keys [filter-content]} {:spell/keys [name description]}]
                     (let [nm (some-> name (str/lower-case))
                           desc (some-> description (str/lower-case))
                           target (some-> filter-content
                                          (str/trim)
                                          (str/lower-case))]
                       (or
                         (nil? target)
                         (empty? target)
                         (and nm (str/includes? nm target))
                         (and desc (str/includes? desc target)))))
   ro/run-on-mount?       true
   ro/initial-sort-params {:sort-by :spell/name
                           :ascending? true
                           :sortable-columns #{:spell/name}}
   ro/controls            {::search! {:type :button
                                      :local? true
                                      :label " Filter "
                                      :class " ui basic compact mini red button "
                                      :action (fn [this _] (report/filter-rows! this))}
                           ::filter-content {:type :string
                                             :local? true
                                             :placeholder " Type a partial spell name or description "
                                             :onChange (fn [this _] (report/filter-rows! this))}}
   ro/control-layout      {:action-buttons []
                           :inputs         [[::filter-content ::search! :_]]}
   })

(def ui-spell-list (comp/factory SpellList))
