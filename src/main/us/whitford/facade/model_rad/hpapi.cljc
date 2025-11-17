(ns us.whitford.facade.model-rad.hpapi
  "RAD definition of Harry Potter API entities. Attributes only."
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]))

;; Character attributes

(defattr character_id :character/id :string
  {ao/identity? true
   ao/required? true})

(defattr character_name :character/name :string
  {ao/identities #{:character/id}})

(defattr character_alternate_names :character/alternate_names :string
  {ao/identities #{:character/id}})

(defattr character_species :character/species :string
  {ao/identities #{:character/id}})

(defattr character_gender :character/gender :string
  {ao/identities #{:character/id}})

(defattr character_house :character/house :string
  {ao/identities #{:character/id}})

(defattr character_dateOfBirth :character/dateOfBirth :string
  {ao/identities #{:character/id}})

(defattr character_yearOfBirth :character/yearOfBirth :string
  {ao/identities #{:character/id}})

(defattr character_wizard :character/wizard :boolean
  {ao/identities #{:character/id}})

(defattr character_ancestry :character/ancestry :string
  {ao/identities #{:character/id}})

(defattr character_eyeColour :character/eyeColour :string
  {ao/identities #{:character/id}})

(defattr character_hairColour :character/hairColour :string
  {ao/identities #{:character/id}})

(defattr character_patronus :character/patronus :string
  {ao/identities #{:character/id}})

(defattr character_hogwartsStudent :character/hogwartsStudent :boolean
  {ao/identities #{:character/id}})

(defattr character_hogwartsStaff :character/hogwartsStaff :boolean
  {ao/identities #{:character/id}})

(defattr character_actor :character/actor :string
  {ao/identities #{:character/id}})

(defattr character_alive :character/alive :boolean
  {ao/identities #{:character/id}})

(defattr character_image :character/image :string
  {ao/identities #{:character/id}})

(def character-attributes
  [character_id character_name character_alternate_names character_species
   character_gender character_house character_dateOfBirth character_yearOfBirth
   character_wizard character_ancestry character_eyeColour character_hairColour
   character_patronus character_hogwartsStudent character_hogwartsStaff
   character_actor character_alive character_image])

;; Spell attributes

(defattr spell_id :spell/id :string
  {ao/identity? true
   ao/required? true})

(defattr spell_name :spell/name :string
  {ao/identities #{:spell/id}})

(defattr spell_description :spell/description :string
  {ao/identities #{:spell/id}})

(def spell-attributes
  [spell_id spell_name spell_description])

(def attributes (vec (concat character-attributes spell-attributes)))
