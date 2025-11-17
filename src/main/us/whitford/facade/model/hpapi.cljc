(ns us.whitford.facade.model.hpapi
  "Functions, resolvers, and mutations supporting Harry Potter API.

   DO NOT require a RAD model file in this ns. This ns is meant to be an ultimate
   leaf of the requires. Only include library code."
  (:require
   #?@(:clj [[us.whitford.facade.components.hpapi :refer [hpapi-martian]]
             [us.whitford.facade.components.config :refer [config]]])
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [com.fulcrologic.fulcro.mutations :as m]
   [com.fulcrologic.rad.ids :refer [new-uuid]]
   [com.wsscode.pathom3.connect.operation :as pco]
   [martian.core :as martian]
   [taoensso.timbre :as log]
   [us.whitford.facade.components.utils :refer [map->nsmap map->deepnsmap str->int
                                                update-in-contains]])
  #?(:clj (:import [java.util UUID])))

(comment
  (tap> {:from :repl :hpapi-martian hpapi-martian})
  (martian/explore hpapi-martian)
  (martian/explore hpapi-martian :characters))

#?(:clj
   (defn hpapi-data
     "Fetch data from Harry Potter API. Returns nil on error."
     [op {:keys [search] :as params}]
     (let [op-map {:spells "spell"
                   :characters "character"}
           ops (set (keys op-map))]
       (when (some ops [op])
         (try
           (let [search? (boolean search)
                 {:keys [body status]} @(martian/response-for hpapi-martian op params)]
             (if (= 200 status)
               (if search?
                 (->> (filterv #(let [name-match (when (:name %)
                                                   (str/includes? (str/lower-case (:name %))
                                                                  (str/lower-case search)))]
                                  name-match)
                               body)
                      (mapv #(map->nsmap % (op-map op))))
                 (->> (vec body)
                      (mapv #(map->nsmap % (op-map op)))))
               (do
                 (log/error "HPAPI API error" {:operation op :status status :body body})
                 nil)))
           (catch Exception e
             (log/error e "Failed to fetch HPAPI data" {:operation op :params params})
             nil))))))

(comment
  (some #{:characters :spells} :spells)
  (hpapi-data :characters {:search "Harry"}))

#?(:clj
   (pco/defresolver all-characters-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:hpapi/all-characters [:character/id :character/name :character/house
                                            :character/species :character/ancestry]}]}
     (try
       (let [{:keys [search]} query-params
             opts (cond-> {}
                    search (assoc :search search))]
         {:hpapi/all-characters (or (hpapi-data :characters opts) [])})
       (catch Exception e
         (log/error e "Failed to resolve all-characters")
         {:hpapi/all-characters []}))))

#?(:clj
   (pco/defresolver character-resolver [{:keys [character/id] :as params}]
     {::pco/input [:character/id]
      ::pco/output [:character/name :character/house :character/species :character/gender
                    :character/dateOfBirth :character/yearOfBirth :character/wizard
                    :character/ancestry :character/eyeColour :character/hairColour
                    :character/patronus :character/hogwartsStudent :character/hogwartsStaff
                    :character/actor :character/alive :character/image]}
     (try
       (let [{:keys [body status]} @(martian/response-for hpapi-martian :characters {})]
         (if (= 200 status)
           (let [characters (mapv #(map->nsmap % "character") body)
                 character (first (filter #(= id (:character/id %)) characters))]
             (or (dissoc character :character/id) {}))
           (do
             (log/error "HPAPI character lookup error" {:id id :status status})
             {})))
       (catch Exception e
         (log/error e "Failed to resolve character" {:id id})
         {}))))

#?(:clj
   (pco/defresolver all-spells-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:hpapi/all-spells [:spell/id :spell/name :spell/description]}]}
     (try
       (let [{:keys [search]} query-params
             opts (cond-> {}
                    search (assoc :search search))]
         {:hpapi/all-spells (or (hpapi-data :spells opts) [])})
       (catch Exception e
         (log/error e "Failed to resolve all-spells")
         {:hpapi/all-spells []}))))

#?(:clj
   (pco/defresolver spell-resolver [{:keys [spell/id] :as params}]
     {::pco/input [:spell/id]
      ::pco/output [:spell/name :spell/description]}
     (try
       (let [{:keys [body status]} @(martian/response-for hpapi-martian :spells {})]
         (if (= 200 status)
           (let [spells (mapv #(map->nsmap % "spell") body)
                 spell (first (filter #(= id (:spell/id %)) spells))]
             (or (dissoc spell :spell/id) {}))
           (do
             (log/error "HPAPI spell lookup error" {:id id :status status})
             {})))
       (catch Exception e
         (log/error e "Failed to resolve spell" {:id id})
         {}))))

#?(:clj (def resolvers [all-characters-resolver
                        character-resolver
                        all-spells-resolver
                        spell-resolver]))

(comment
  (martian/explore hpapi-martian :characters)
  (martian/explore hpapi-martian :spells)
  @(martian/response-for hpapi-martian :characters)
  @(martian/response-for hpapi-martian :spells))
