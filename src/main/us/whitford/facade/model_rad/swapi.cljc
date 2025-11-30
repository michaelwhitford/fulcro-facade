(ns us.whitford.facade.model-rad.swapi
  "RAD definition of a `model`. Attributes only. These will be used all over the app, so try to limit
   requires to model code and library code."
  (:require
    [clojure.spec.alpha :as spec]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.form-options :as fo]))

;; NOTE: Entity attributes moved to model_rad/entity.cljc

;; person :swapi/all-people

(defattr person_id :person/id :string
  {ao/identity? true
   ao/required? true})

(defattr person_name :person/name :string
  {ao/identities #{:person/id}})

(defattr person_birth_year :person/birth_year :string
  {ao/identities #{:person/id}})

(defattr person_eye_color :person/eye_color :string
  {ao/identities #{:person/id}})

(defattr person_films :person/films :ref
  {ao/identities #{:person/id}
   ao/target :film/id
   ao/cardinality :many})

(defattr person_gender :person/gender :string
  {ao/identities #{:person/id}})

(defattr person_hair_color :person/hair_color :string
  {ao/identities #{:person/id}})

(defattr person_height :person/height :string
  {ao/identities #{:person/id}})

(defattr person_homeworld :person/homeworld :ref
  {ao/identities #{:person/id}
   ao/target :planet/id
   ao/cardinality :one
   })

(defattr person_mass :person/mass :string
  {ao/identities #{:person/id}})

(defattr person_skin_color :person/skin_color :string
  {ao/identities #{:person/id}})

(def person-attributes
  [person_id person_name #_person_birth_year #_person_eye_color #_person_films
   #_person_gender #_person_hair_color #_person_height #_person_homeworld
   #_person_mass #_person_skin_color])

;; film :swapi/all-films

(defattr film_id :film/id :string
  {ao/identity? true
   ao/required? true})

(defattr film_title :film/title :string
  {ao/identities #{:film/id}})

(defattr film_characters :film/characters :ref
  {ao/target :film/id
   ao/identities #{:film/id}
   ao/cardinality :many})

(defattr film_director :film/director :string
  {ao/identities #{:film/id}})

(defattr film_episode_id :film/episode_id :int
  {ao/identities #{:film/id}})

(defattr film_opening_crawl :film/opening_crawl :string
  {ao/identities #{:film/id}})

(defattr film_planets :film/planets :ref
  {ao/target :planet/id
   ao/identities #{:film/id}
   ao/cardinality :many})

(defattr film_producer :film/producer :string
  {ao/identities #{:film/id}})

(defattr film_release_date :film/release_date :string
  {ao/identities #{:film/id}})

(defattr film_species :film/species :ref
  {ao/target :specie/id
   ao/identities #{:film/id}
   ao/cardinality :many})

(defattr film_starships :film/starships :ref
  {ao/target :spaceship/id
   ao/identities #{:film/id}
   ao/cardinality :many})

(defattr film_vehicles :film/vehicles :ref
  {ao/target :vehicle/id
   ao/cardinality :many})

(def film-attributes
  [film_id film_title film_characters film_director film_episode_id
   film_opening_crawl film_planets film_producer film_release_date
   film_species film_starships film_vehicles])

;; planet :swapi/all-planets

(defattr planet_id :planet/id :string
  {ao/identity? true})

(defattr planet_name :planet/name :string
  {ao/identities #{:planet/id}})

(defattr planet_climate :planet/climate :string
  {ao/identities #{:planet/id}})

(defattr planet_gravity :planet/gravity :string
  {ao/identities #{:planet/id}})

(defattr planet_diameter :planet/diameter :string
  {ao/identities #{:planet/id}})

(defattr planet_orbital_period :planet/orbital_period :string
  {ao/identities #{:planet/id}})

(defattr planet_population :planet/population :string
  {ao/identities #{:planet/id}})

(defattr planet_rotation_period :planet/rotation_period :string
  {ao/identities #{:planet/id}})

(defattr planet_terrain :planet/terrain :string
  {ao/identities #{:planet/id}})

(def planet-attributes
  [planet_id planet_name planet_climate planet_gravity planet_diameter
   planet_orbital_period planet_population planet_rotation_period planet_terrain])

;; species :swapi/all-species (:specie namespace is there a better way?)

(defattr species_id :specie/id :string
  {ao/identity? true})

(defattr species_name :specie/name :string
  {ao/identities #{:specie/id}})

(defattr species_average_height :specie/average_height :string
  {ao/identities #{:specie/id}})

(defattr species_average_lifespan :specie/average_lifespan :string
  {ao/identities #{:specie/id}})

(defattr species_classification :specie/classification :string
  {ao/identities #{:specie/id}})

(defattr species_designation :specie/designation :string
  {ao/identities #{:specie/id}})

(defattr species_eye_colors :specie/eye_colors :string
  {ao/identities #{:specie/id}})

(defattr species_films :specie/films :ref
  {ao/target :film/id
   ao/identities #{:specie/id}
   ao/cardinality :many})

(defattr species_hair_colors :specie/hair_colors :string
  {ao/identities #{:specie/id}})

(defattr species_homeworld :specie/homeworld :ref
  {ao/target :planet/id
   ao/identities #{:specie/id}
   ao/cardinality :one})

(defattr species_language :specie/language :string
  {ao/identities #{:specie/id}})

(defattr species_people :specie/people :ref
  {ao/target :person/id
   ao/identities #{:specie/id}
   ao/cardinality :many})

(defattr species_skin_colors :specie/skin_colors :string
  {ao/identities #{:specie/id}})

(def species-attributes
  [species_id species_name species_average_height species_average_lifespan
   species_classification species_designation species_eye_colors
   species_films species_hair_colors species_homeworld species_language
   species_people species_skin_colors])

;; vehicle :swapi/all-vehicles

(defattr vehicle_id :vehicle/id :string
  {ao/identity? true})

(defattr vehicle_name :vehicle/name :string
  {ao/identities #{:vehicle/id}})

(defattr vehicle_capacity :vehicle/cargo_capacity :string
  {ao/identities #{:vehicle/id}})

(defattr vehicle_consumables :vehicle/consumables :string
  {ao/identities #{:vehicle/id}})

(defattr vehicle_cost_in_credits :vehicle/cost_in_credits :string
  {ao/identities #{:vehicle/id}})

(defattr vehicle_crew :vehicle/crew :string
  {ao/identities #{:vehicle/id}})

(defattr vehicle_films :vehicle/films :ref
  {ao/target :film/id
   ao/identities #{:vehicle/id}
   ao/cardinality :many})

(defattr vehicle_model :vehicle/model :string
  {ao/identities #{:vehicle/id}})

(defattr vehicle_manufacturer :vehicle/manufacturer :string
  {ao/identities #{:vehicle/id}})

(defattr vehicle_passengers :vehicle/passengers :string
  {ao/identities #{:vehicle/id}})

(defattr vehicle_pilots :vehicle/pilots :ref
  {ao/target :person/id
   ao/identities #{:vehicle/id}
   ao/cardinality :many})

(def vehicle-attributes
  [vehicle_id vehicle_name vehicle_capacity vehicle_consumables vehicle_cost_in_credits
   vehicle_crew vehicle_films vehicle_model vehicle_manufacturer vehicle_passengers
   vehicle_pilots])

;; starship :swapi/all-starships

(defattr starship_id :starship/id :string
  {ao/identity? true})

(defattr starship_name :starship/name :string
  {ao/identities #{:starship/id}})

(defattr starship_cargo_capacity :starship/cargo_capacity :string
  {ao/identities #{:starship/id}})

(defattr starship_consumables :starship/consumables :string
  {ao/identities #{:starship/id}})

(defattr starship_cost_in_credits :starship/cost_in_credits :string
  {ao/identities #{:starship/id}})

(defattr starship_crew :starship/crew :string
  {ao/identities #{:starship/id}})

(defattr starship_films :starship/films :ref
  {ao/target :film/id
   ao/identities #{:starship/id}
   ao/cardinality :many})

(defattr starship_hyperdrive_rating :starship/hyperdrive_rating :string
  {ao/identities #{:starship/id}})

(defattr starship_length :starship/length :string
  {ao/identities #{:starship/id}})

(defattr starship_manufacturer :starship/manufacturer :string
  {ao/identities #{:starship/id}})

(defattr starship_max_atmosphering_speed :starship/max_atmosphering_speed :string
  {ao/identities #{:starship/id}})

(defattr starship_model :starship/model :string
  {ao/identities #{:starship/id}})

(defattr starship_passengers :starship/passengers :string
  {ao/identities #{:starship/id}})

(defattr starship_pilots :starship/pilots :ref
  {ao/target :person/id
   ao/identities #{:starship/id}
   ao/cardinality :many})

(defattr starship_class :starship/class :string
  {ao/identities #{:starship/id}})

(def starship-attributes
  [starship_id starship_name starship_cargo_capacity starship_consumables
   starship_cost_in_credits starship_crew starship_films starship_hyperdrive_rating
   starship_length starship_manufacturer starship_max_atmosphering_speed starship_model
   starship_passengers starship_pilots starship_class])

(def attributes (vec (concat person-attributes
                             planet-attributes
                             species-attributes
                             film-attributes
                             vehicle-attributes
                             starship-attributes)))
