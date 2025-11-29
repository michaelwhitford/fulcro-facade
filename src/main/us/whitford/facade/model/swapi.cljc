(ns us.whitford.facade.model.swapi
  "Functions, resolvers, and mutations supporting `swapi`.

   DO NOT require a RAD model file in this ns. This ns is meant to be an ultimate
   leaf of the requires. Only include library code."
  (:require
   #?@(:clj [[us.whitford.facade.components.swapi :refer [swapi-martian]]
             [us.whitford.facade.components.config :refer [config]]])
   [clojure.math :refer [ceil]]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [com.fulcrologic.fulcro.mutations :as m]
   [com.fulcrologic.rad.ids :refer [new-uuid]]
   [com.fulcrologic.statecharts.integration.fulcro.rad-integration :as ri]
   [com.wsscode.pathom3.connect.operation :as pco]
   [martian.core :as martian]
   [taoensso.timbre :as log]
   [us.whitford.facade.components.utils :refer [map->nsmap map->deepnsmap str->int
                                                update-in-contains]])
  #?(:clj (:import [java.util UUID])))

(defn swapiurl->id
  "extract the id from a swapi.dev url string"
  [s]
  (let [m (re-matches #"^https://swapi.dev/api/.*/(\d+)/$" s)]
    (when (seq m)
      (str (second m)))))

(defn swapi-id
  "use url value to create an id for pathom"
  [m]
  (if-let [url (:url m)]
    (assoc m :id (swapiurl->id url))
    m))

(defn swapi-page->number
  "extract the swapi page number"
  ([s]
   (let [m (re-matches #"^.*page=(\d+).*$" s)]
     (when (seq m)
       (str->int (second m))))))

(comment
  (swapi-page->number "https://swapi.dev/api/people/?page=2") ; => 2
  )

(defn swapi->pathom
  "transform swapi map with urls into ids for pathom and martian api client"
  ([m]
   (let [objs #{:films :starships :species :vehicles :residents
                :people :characters :pilots :planets :homeworld}]
     (reduce (fn [acc obj]
               (if-let [val (get acc obj)]
                 (update acc obj (fn [i]
                                   (cond
                                     (coll? i) (remove nil? (mapv #(when (string? %) (swapiurl->id %)) i))
                                     (string? i) (swapiurl->id i)
                                     :else nil)))
                 acc)) m objs))))

(defn transform-swapi
  [input]
  (->> (mapv swapi-id input)
       #_(mapv #(swapi->pathom %))
       (mapv (fn [m] (update-in-contains m [:films] (fn [i] (mapv #(swapiurl->id %) i)))))
       (mapv (fn [m] (update-in-contains m [:starships] (fn [i] (mapv #(swapiurl->id %) i)))))
       (mapv (fn [m] (update-in-contains m [:species] (fn [i] (mapv #(swapiurl->id %) i)))))
       (mapv (fn [m] (update-in-contains m [:vehicles] (fn [i] (mapv #(swapiurl->id %) i)))))
       (mapv (fn [m] (update-in-contains m [:residents] (fn [i] (mapv #(swapiurl->id %) i)))))
       (mapv (fn [m] (update-in-contains m [:people] (fn [i] (mapv #(swapiurl->id %) i)))))
       (mapv (fn [m] (update-in-contains m [:characters] (fn [i] (mapv #(swapiurl->id %) i)))))
       (mapv (fn [m] (update-in-contains m [:pilots] (fn [i] (mapv #(swapiurl->id %) i)))))
       (mapv (fn [m] (update-in-contains m [:planets] (fn [i] (mapv #(swapiurl->id %) i)))))
       (mapv (fn [m] (update-in-contains m [:homeworld] (fn [i] (swapiurl->id i)))))))

#?(:clj
   (defn swapi-step-fn
     "step function for paginated results"
     [{:keys [iteration entitytype opts final] :as data}]
     (let [id-map {:people :person
                   :vehicles :vehicle
                   :films :film
                   :planets :planet
                   :starships :starship
                   :species :specie}
           allowed (set (keys id-map))
           {:keys [id search]} opts]
       (when (some allowed [entitytype]) ; invalid type will short circuit iteration
         (if id ;  single-object
           (let [{:keys [status body]} @(martian/response-for swapi-martian (id-map entitytype) opts)]
             (when (= 200 status)
               {:iteration iteration
                :entitytype entitytype
                :opts opts
                :final final
                :next-token {:iteration (inc iteration)
                             :entitytype :none ; next iteration fail from allowed type check
                             :opts opts
                             :final {:results [body]}}}))
                 ; search or all with possible pagination
           (let [r @(martian/response-for swapi-martian entitytype opts)
                 {:keys [status body]} r]
             (when (= 200 status)
               (let [{:keys [count next previous results]} body
                     page-size 10
                     max-iterations (int (ceil (/ count page-size)))]
                 (when (<= iteration max-iterations)
                   (Thread/sleep (+ 3000 (rand-int 4001)))
                   {:iteration iteration
                    :entitytype entitytype
                    :opts opts
                    :final final
                    :next-token {:iteration (inc iteration)
                                 :entitytype entitytype
                                 :opts (if (nil? next)
                                         opts
                                         (assoc opts :page (swapi-page->number next)))
                                 :final body}})))))))))

(comment
  (swapi-step-fn {:iteration 1 :entitytype :people :opts {:id "1"} :final {}}))

#?(:clj
   (defn get-swapi
     "get swapi object(s) from api"
     [{:keys [id search entitytype] :as params}]
     (let [swapi-opts (cond-> {}
                        id (assoc :id (str id))
                        search (assoc :search (str search)))]
       (tap> {:from ::get-swapi :params params :swapi-opts swapi-opts})
       (vec (flatten (vec (iteration swapi-step-fn
                                     :somef (fn [res]
                                              #_(tap> {:from ::get-swapi-somef :res res})
                                              (some? (get-in res [:next-token :final :results])))
                                     :vf (fn [res]
                                           #_(tap> {:from ::get-swapi-vf :res res})
                                           (get-in res [:next-token :final :results]))
                                     :kf :next-token
                                     :initk {:iteration 1
                                             :entitytype entitytype
                                             :opts swapi-opts
                                             :final {}})))))))

(comment
  (get-swapi {:entitytype :people}))

#?(:clj
   (defn swapi-data
     "Fetch SWAPI data for a given entity type. Returns nil on error."
     [ent swapi-opts]
     (let [id-map {:people :person
                   :vehicles :vehicle
                   :films :film
                   :planets :planet
                   :starships :starship
                   :species :specie}
           values (set (vals id-map))]
       (try
         (let [{:keys [status body]} @(martian/response-for swapi-martian ent swapi-opts)]
           (if (= 200 status)
             (let [result
                   (if (values ent)
                     (->> [body]
                          transform-swapi
                          (mapv #(map->nsmap % (clojure.core/name ent)))
                          first)
                     (->> body
                          :results
                          transform-swapi
                          (mapv #(map->nsmap % (clojure.core/name (id-map ent))))))]
               result)
             (do
               (log/error "SWAPI API error" {:entity ent :status status :body body})
               nil)))
         (catch Exception e
           (log/error e "Failed to fetch SWAPI data" {:entity ent :opts swapi-opts})
           nil)))))

#?(:clj
   (defn swapi-data-paginated
     "Fetch SWAPI data with pagination metadata.
      Returns {:results [...] :total-count n :current-page p :page-size 10}
      Returns empty results on error."
     [ent swapi-opts]
     (let [id-map {:people :person
                   :vehicles :vehicle
                   :films :film
                   :planets :planet
                   :starships :starship
                   :species :specie}
           values (set (vals id-map))
           default-empty {:results []
                          :total-count 0
                          :current-page (or (:page swapi-opts) 1)
                          :page-size 10}]
       (try
         (let [{:keys [status body]} @(martian/response-for swapi-martian ent swapi-opts)]
           (if (= 200 status)
             (if (values ent)
               ;; Single entity lookup
               {:results (->> [body]
                              transform-swapi
                              (mapv #(map->nsmap % (clojure.core/name ent)))
                              first)
                :total-count 1
                :current-page 1
                :page-size 10}
               ;; List with pagination
               (let [{:keys [count results next previous]} body
                     current-page (or (:page swapi-opts) 1)
                     transformed-results (->> results
                                              transform-swapi
                                              (mapv #(map->nsmap % (clojure.core/name (id-map ent)))))]
                 {:results transformed-results
                  :total-count count
                  :current-page current-page
                  :page-size 10}))
             (do
               (log/error "SWAPI paginated API error" {:entity ent :status status :body body})
               default-empty)))
         (catch Exception e
           (log/error e "Failed to fetch paginated SWAPI data" {:entity ent :opts swapi-opts})
           default-empty)))))

(comment
  (martian/explore swapi-martian :person)
  (martian/explore swapi-martian :people)
  (swapi-data :people {})
  (swapi-data :person {:id (str 1)})
  (swapi-data :film  {:id "1"})
  (swapi-data :planet {:id "1"})
  (swapi-data :specie {:id "1"})
  (swapi-data :vehicle {:id "4"})
  (swapi-data :starships {}))

#?(:clj
   (pco/defresolver all-people-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-people [:person/id :person/name :person/birth_year :person/eye_color
                                        :person/films :person/gender :person/hair_color :person/height
                                        :person/homeworld :person/mass :person/skin_color]}
                    :swapi.people/total-count
                    :swapi.people/current-page
                    :swapi.people/page-size]}
     (try
       (let [{:keys [search page]} query-params
             page-num (if (string? page) (str->int page) (or page 1))
             opts (cond-> {}
                    search (assoc :search search)
                    page-num (assoc :page page-num))
             {:keys [results total-count current-page page-size]} (swapi-data-paginated :people opts)]
             (tap> {:from ::all-people-resolver :pathom-env env})
         {:swapi/all-people (or results [])
          :swapi.people/total-count (or total-count 0)
          :swapi.people/current-page (or current-page 1)
          :swapi.people/page-size (or page-size 10)})
       (catch Exception e
         (log/error e "Failed to resolve all-people")
         {:swapi/all-people []
          :swapi.people/total-count 0
          :swapi.people/current-page 1
          :swapi.people/page-size 10}))))

#?(:clj
   (pco/defresolver person-resolver [env {:person/keys [id] :as params}]
     {::pco/output [:person/id :person/name :person/birth_year :person/eye_color
                    :person/films :person/gender :person/hair_color :person/height
                    :person/homeworld :person/mass :person/skin_color]}
     (try
       (or (swapi-data :person {:id (str id)}) {})
       (catch Exception e
         (log/error e "Failed to resolve person" {:id id})
         {}))))

#?(:clj
   (pco/defresolver all-vehicles-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-vehicles [:vehicle/id :vehicle/name :vehicle/cargo_capacity :vehicle/consumables
                                          :vehicle/cost_in_credits :vehicle/crew :vehicle/films :vehicle/model
                                          :vehicle/manufacturer :vehicle/passengers :vehicle/pilots]}
                    :swapi.vehicles/total-count
                    :swapi.vehicles/current-page
                    :swapi.vehicles/page-size]}
     (try
       (let [{:keys [search page]} query-params
             page-num (if (string? page) (str->int page) (or page 1))
             opts (cond-> {}
                    search (assoc :search search)
                    page-num (assoc :page page-num))
             {:keys [results total-count current-page page-size]} (swapi-data-paginated :vehicles opts)]
         {:swapi/all-vehicles (or results [])
          :swapi.vehicles/total-count (or total-count 0)
          :swapi.vehicles/current-page (or current-page 1)
          :swapi.vehicles/page-size (or page-size 10)})
       (catch Exception e
         (log/error e "Failed to resolve all-vehicles")
         {:swapi/all-vehicles []
          :swapi.vehicles/total-count 0
          :swapi.vehicles/current-page 1
          :swapi.vehicles/page-size 10}))))

#?(:clj
   (pco/defresolver vehicle-resolver [env {:vehicle/keys [id] :as params}]
     {::pco/output [:vehicle/id :vehicle/name :vehicle/cargo_capacity :vehicle/consumables
                    :vehicle/cost_in_credits :vehicle/crew :vehicle/films :vehicle/model
                    :vehicle/manufacturer :vehicle/passengers :vehicle/pilots]}
     (try
       (or (swapi-data :vehicle {:id (str id)}) {})
       (catch Exception e
         (log/error e "Failed to resolve vehicle" {:id id})
         {}))))

#?(:clj
   (pco/defresolver all-starships-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-starships
                     [:starship/id :starship/name :starship/cargo_capacity :starship/consumables
                      :starship/cost_in_credits :starship/crew :starship/films :starship/hyperdrive_rating
                      :starship/length :starship/manufacturer :starship/max_atmosphering_speed
                      :starship/model :starship/passengers :starship/pilots :starship/class]}
                    :swapi.starships/total-count
                    :swapi.starships/current-page
                    :swapi.starships/page-size]}
     (try
       (let [{:keys [search page]} query-params
             page-num (if (string? page) (str->int page) (or page 1))
             opts (cond-> {}
                    search (assoc :search search)
                    page-num (assoc :page page-num))
             {:keys [results total-count current-page page-size]} (swapi-data-paginated :starships opts)]
         {:swapi/all-starships (or results [])
          :swapi.starships/total-count (or total-count 0)
          :swapi.starships/current-page (or current-page 1)
          :swapi.starships/page-size (or page-size 10)})
       (catch Exception e
         (log/error e "Failed to resolve all-starships")
         {:swapi/all-starships []
          :swapi.starships/total-count 0
          :swapi.starships/current-page 1
          :swapi.starships/page-size 10}))))

#?(:clj
   (pco/defresolver starship-resolver [env {:starship/keys [id] :as params}]
     {::pco/output [:starship/id :starship/name :starship/cargo_capacity :starship/consumables
                    :starship/cost_in_credits :starship/crew :starship/films :starship/hyperdrive_rating
                    :starship/length :starship/manufacturer :starship/max_atmosphering_speed
                    :starship/model :starship/passengers :starship/pilots :starship/class]}
     (try
       (or (swapi-data :starship {:id (str id)}) {})
       (catch Exception e
         (log/error e "Failed to resolve starship" {:id id})
         {}))))

#?(:clj
   (pco/defresolver all-films-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-films [:film/id :film/title :film/characters :film/director :film/episode_id
                                       :film/opening_crawl :film/planets :film/producer :film/release_date
                                       :film/species :film/starships :film/vehicles]}
                    :swapi.films/total-count
                    :swapi.films/current-page
                    :swapi.films/page-size]}
     (try
       (let [{:keys [search page]} query-params
             page-num (if (string? page) (str->int page) (or page 1))
             opts (cond-> {}
                    search (assoc :search search)
                    page-num (assoc :page page-num))
             {:keys [results total-count current-page page-size]} (swapi-data-paginated :films opts)]
         {:swapi/all-films (or results [])
          :swapi.films/total-count (or total-count 0)
          :swapi.films/current-page (or current-page 1)
          :swapi.films/page-size (or page-size 10)})
       (catch Exception e
         (log/error e "Failed to resolve all-films")
         {:swapi/all-films []
          :swapi.films/total-count 0
          :swapi.films/current-page 1
          :swapi.films/page-size 10}))))

#?(:clj
   (pco/defresolver film-resolver [env {:film/keys [id] :as params}]
     {::pco/output [:film/id :film/title :film/characters :film/director :film/episode_id
                    :film/opening_crawl :film/planets :film/producer :film/release_date
                    :film/species :film/starships :film/vehicles]}
     (try
       (or (swapi-data :film {:id (str id)}) {})
       (catch Exception e
         (log/error e "Failed to resolve film" {:id id})
         {}))))

#?(:clj
   (pco/defresolver all-species-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-species [:specie/id :specie/name :specie/average_height :specie/average_lifespan
                                         :specie/classification :specie/designation :specie/eye_colors :specie/films
                                         :specie/hair_colors :specie/homeworld :specie/language :specie/people
                                         :specie/skin_colors]}
                    :swapi.species/total-count
                    :swapi.species/current-page
                    :swapi.species/page-size]}
     (try
       (let [{:keys [search page]} query-params
             page-num (if (string? page) (str->int page) (or page 1))
             opts (cond-> {}
                    search (assoc :search search)
                    page-num (assoc :page page-num))
             {:keys [results total-count current-page page-size]} (swapi-data-paginated :species opts)]
         {:swapi/all-species (or results [])
          :swapi.species/total-count (or total-count 0)
          :swapi.species/current-page (or current-page 1)
          :swapi.species/page-size (or page-size 10)})
       (catch Exception e
         (log/error e "Failed to resolve all-species")
         {:swapi/all-species []
          :swapi.species/total-count 0
          :swapi.species/current-page 1
          :swapi.species/page-size 10}))))

#?(:clj
   (pco/defresolver species-resolver [env {:specie/keys [id] :as params}]
     {::pco/output [:specie/id :specie/name :specie/average_height :specie/average_lifespan
                    :specie/classification :specie/designation :specie/eye_colors :specie/films
                    :specie/hair_colors :specie/homeworld :specie/language :specie/people
                    :specie/skin_colors]}
     (try
       (or (swapi-data :specie {:id (str id)}) {})
       (catch Exception e
         (log/error e "Failed to resolve species" {:id id})
         {}))))

#?(:clj
   (pco/defresolver all-planets-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-planets [:planet/id :planet/name :planet/climate :planet/gravity
                                         :planet/diameter :planet/orbital_period :planet/population
                                         :planet/rotation_period :planet/terrain]}
                    :swapi.planets/total-count
                    :swapi.planets/current-page
                    :swapi.planets/page-size]}
     (try
       (let [{:keys [search page]} query-params
             page-num (if (string? page) (str->int page) (or page 1))
             opts (cond-> {}
                    search (assoc :search search)
                    page-num (assoc :page page-num))
             {:keys [results total-count current-page page-size]} (swapi-data-paginated :planets opts)]
         {:swapi/all-planets (or results [])
          :swapi.planets/total-count (or total-count 0)
          :swapi.planets/current-page (or current-page 1)
          :swapi.planets/page-size (or page-size 10)})
       (catch Exception e
         (log/error e "Failed to resolve all-planets")
         {:swapi/all-planets []
          :swapi.planets/total-count 0
          :swapi.planets/current-page 1
          :swapi.planets/page-size 10}))))

#?(:clj
   (pco/defresolver planet-resolver [env {:planet/keys [id] :as params}]
     {::pco/output [:planet/id :planet/name :planet/climate :planet/gravity
                    :planet/diameter :planet/orbital_period :planet/population
                    :planet/rotation_period :planet/terrain]}
     (try
       (or (swapi-data :planet {:id (str id)}) {})
       (catch Exception e
         (log/error e "Failed to resolve planet" {:id id})
         {}))))

#?(:clj
   (pco/defmutation search [env params]
     {::pco/input [:search]}
     (try
       (tap> {:from ::search :env env :params params})
       (catch Exception e
         (log/error "swapi-search error" e))))
   :cljs
   (m/defmutation search [{:keys [search] :as params}]
     (remote [env] (m/returning env :us.whitford.facade.ui.search-forms/SearchReport))
     (ok-action [{:keys [app result] :as env}]
                (ri/create! app :us.whitford.facade.ui.search-forms/SearchReport))))

#?(:clj
   (pco/defmutation load-person
     "load a person into a form"
     [env {:person/keys [id] :as params}]
     {::pco/input [:person/id]}
     (let [r (first (swapi-data :person {:id (str id)}))]
       (tap> {:from ::load-person-clj :params params :r r})
       (let [new-id (new-uuid (+ 5000 (str->int (:person/id r))))]
         (assoc r :tempids (hash-map (tempid/tempid) new-id)))))
   :cljs
   (m/defmutation load-person [params]
     (remote [env] (m/returning env :us.whitford.facade.ui.swapi-forms/PersonForm))
     (ok-action [{:keys [app result] :as env}]
                (ri/create! app :us.whitford.facade.ui.swapi-forms/PersonForm
                            (let [r (get-in result [:body `load-person])]
                              (tap> {:from ::load-person-cljs :params params :r r :result result})
           ; remove id, pathom queries the db if there is an id
                              {:initial-state  (dissoc r :person/id)})))))

#?(:clj
   (defn fetch-and-transform-entities
     "Fetch entities from SWAPI with search term and transform to unified format.
      Films don't support search param, so we filter client-side."
     [search-term]
     (let [;; Entity types that support SWAPI search parameter
           searchable-types [:people :vehicles :planets :species :starships]
           ;; Films don't support search, need client-side filtering
           non-searchable-types [:films]

           ;; Build opts for searchable types
           search-opts (if (and search-term (not (str/blank? search-term)))
                         {:search search-term}
                         {})

           ;; Fetch searchable types with search param (in parallel)
           searchable-futures (doall
                                (map (fn [ent-type]
                                       (future (try
                                                 (swapi-data ent-type search-opts)
                                                 (catch Exception e
                                                   (log/warn "Failed to fetch" ent-type e)
                                                   []))))
                                     searchable-types))

           ;; Fetch non-searchable types without search param
           non-searchable-futures (doall
                                    (map (fn [ent-type]
                                           (future (try
                                                     (swapi-data ent-type {})
                                                     (catch Exception e
                                                       (log/warn "Failed to fetch" ent-type e)
                                                       []))))
                                         non-searchable-types))

           ;; Wait for all results
           searchable-results (mapcat deref searchable-futures)
           non-searchable-results (mapcat deref non-searchable-futures)

           ;; Filter non-searchable results client-side if search term provided
           filtered-non-searchable (if (and search-term (not (str/blank? search-term)))
                                     (let [search-lower (str/lower-case search-term)]
                                       (filter (fn [entity]
                                                 (let [name-field (or (:film/title entity)
                                                                      (:film/name entity)
                                                                      "")]
                                                   (str/includes? (str/lower-case (str name-field))
                                                                  search-lower)))
                                               non-searchable-results))
                                     non-searchable-results)]

       (concat searchable-results filtered-non-searchable))))

#?(:clj
   (pco/defresolver all-entities-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-entities [:entity/id :entity/name :entity/type]}]}
     (try
       (let [{:keys [search]} query-params
             all-entities (fetch-and-transform-entities search)
             transformed (keep (fn [m]
                                 (when-let [kw-ns (namespace (first (keys m)))]
                                   (let [entity-id (m (keyword kw-ns "id"))
                                         ;; Handle films which use :title instead of :name
                                         entity-name (or (m (keyword kw-ns "name"))
                                                         (m (keyword kw-ns "title"))
                                                         "Unknown")
                                         entity-type (keyword kw-ns)]
                                     (when (and entity-id entity-type)
                                       {:entity/id (format "%s-%s" kw-ns entity-id)
                                        :entity/name entity-name
                                        :entity/type entity-type}))))
                               all-entities)]
         (log/info "Search completed" {:search-term search :result-count (count transformed)})
         {:swapi/all-entities (vec transformed)})
       (catch Exception e
         (log/error e "Failed to resolve all-entities search")
         {:swapi/all-entities []}))))

#?(:clj (def resolvers [all-people-resolver person-resolver
                        all-vehicles-resolver vehicle-resolver
                        all-starships-resolver starship-resolver
                        all-films-resolver film-resolver
                        all-species-resolver species-resolver
                        all-planets-resolver planet-resolver
                        all-entities-resolver
                        #_load-person
                        #_search]))

(comment
  @(martian/response-for swapi-martian :vehicles {:id "1"})
  @(martian/response-for swapi-martian :planets {:search "tat"})
  @(martian/response-for swapi-martian :vehicles {:id "1"})
  @(martian/response-for swapi-martian :vehicle {:id "4"})
  @(martian/response-for swapi-martian :vehicles {:search "sand"})
  @(martian/response-for swapi-martian :person {:id "1"})
  @(martian/response-for swapi-martian :people {})
  (martian/explore swapi-martian :person)
  (martian/explore swapi-martian :characters)
  (martian/explore swapi-martian :vehicle)
  (martian/explore swapi-martian :people)
  (get-swapi {:entitytype :films}))
