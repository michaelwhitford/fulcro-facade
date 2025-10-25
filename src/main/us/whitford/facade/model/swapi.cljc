(ns us.whitford.facade.model.swapi
  "Functions, resolvers, and mutations supporting `swapi`.

   DO NOT require a RAD model file in this ns. This ns is meant to be an ultimate
   leaf of the requires. Only include library code."
  (:require
    #?@(:clj [[us.whitford.facade.components.swapi :refer [swapi-martian]]
              [us.whitford.facade.components.config :refer [config]]])
    [clojure.math :refer [ceil]]
    [clojure.pprint :refer [pprint]]
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
     (mapv (fn [o]
             (update-in-contains m [o] (fn [i]
                                         (cond
                                           (coll? i) (mapv (fn [x] (swapiurl->id x)) i)
                                           (string? i) (swapiurl->id i)
                                           :else nil)))) objs))))

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
     [ent swapi-opts]
     (let [id-map {:people :person
                   :vehicles :vehicle
                   :films :film
                   :planets :planet
                   :starships :starship
                   :species :specie}
           values (set (vals id-map))
           {:keys [status body]} @(martian/response-for swapi-martian ent swapi-opts)]
       (tap> {:from ::swapi-data :id-map id-map :values values :status status :body body})
       (when (= 200 status)
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
               (tap> {:from ::swapi-data :result result})
               result)))))

(comment
  (martian/explore swapi-martian :person)
  (martian/explore swapi-martian :people)
  (swapi-data :people {})
  (swapi-data :person {:id (str 1)})
  (swapi-data :film  {:id "1"})
  (swapi-data :planet {:id "1"})
  (swapi-data :specie {:id "1"})
  (swapi-data :vehicle {:id "4"})
  (swapi-data :starships {})
  )

#?(:clj
   (pco/defresolver all-people-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-people [:person/id :person/name :person/birth_year :person/eye_color
                                        :person/films :person/gender :person/hair_color :person/height
                                        :person/homeworld :person/mass :person/skin_color]}]}
     (let [{:keys [search page]} query-params
           opts (cond-> {}
                        search (assoc :search search)
                        page (assoc :page page))]
       {:swapi/all-people (swapi-data :people opts)})))

#?(:clj
   (pco/defresolver person-resolver [env {:person/keys [id] :as params}]
     {::pco/output [:person/id :person/name :person/birth_year :person/eye_color
                    :person/films :person/gender :person/hair_color :person/height
                    :person/homeworld :person/mass :person/skin_color]}
     (swapi-data :person {:id (str id)})))

#?(:clj
   (pco/defresolver all-vehicles-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-vehicles [:vehicle/id :vehicle/name :vehicle/cargo_capacity :vehicle/consumables
                                          :vehicle/cost_in_credits :vehicle/crew :vehicle/films :vehicle/model
                                          :vehicle/manufacturer :vehicle/passengers :vehicle/pilots]}]}
     (let [{:keys [search page]} query-params
           opts (cond-> {}
                        search (assoc :search search)
                        page (assoc :page (str page)))]
       {:swapi/all-vehicles (swapi-data :vehicles opts)})))

#?(:clj
   (pco/defresolver vehicle-resolver [env {:vehicle/keys [id] :as params}]
     {::pco/output [:vehicle/id :vehicle/name :vehicle/cargo_capacity :vehicle/consumables
                    :vehicle/cost_in_credits :vehicle/crew :vehicle/films :vehicle/model
                    :vehicle/manufacturer :vehicle/passengers :vehicle/pilots]}
     (swapi-data :vehicle {:id (str id)})))

#?(:clj
   (pco/defresolver all-starships-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-starships
                     [:starship/id :starship/name :starship/cargo_capacity :starship/consumables
                      :starship/cost_in_credits :starship/crew :starship/films :starship/hyperdrive_rating
                      :starship/length :starship/manufacturer :starship/max_atmosphering_speed
                      :starship/model :starship/passengers :starship/pilots :starship/class]}]}
     (let [{:keys [search page]} query-params
           opts (cond-> {}
                        search (assoc :search search)
                        page (assoc :page (str page)))]
       {:swapi/all-starships (swapi-data :starships opts)})))

#?(:clj
   (pco/defresolver starship-resolver [env {:starship/keys [id] :as params}]
     {::pco/output [:starship/id :starship/name :starship/cargo_capacity :starship/consumables
                    :starship/cost_in_credits :starship/crew :starship/films :starship/hyperdrive_rating
                    :starship/length :starship/manufacturer :starship/max_atmosphering_speed
                    :starship/model :starship/passengers :starship/pilots :starship/class]}
     (swapi-data :starship {:id (str id)})))

#?(:clj
   (pco/defresolver all-films-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-films [:film/id :film/title :film/characters :film/director :film/episode_id
                                       :film/opening_crawl :film/planets :film/producer :film/release_date
                                       :film/species :film/starships :film/vehicles]}]}
     (let [{:keys [search page]} query-params
           opts (cond-> {}
                        search (assoc :search search)
                        page (assoc :page (str page)))]
       {:swapi/all-films (swapi-data :films opts)})))

#?(:clj
   (pco/defresolver film-resolver [env {:film/keys [id] :as params}]
     {::pco/output [:film/id :film/title :film/characters :film/director :film/episode_id
                    :film/opening_crawl :film/planets :film/producer :film/release_date
                    :film/species :film/starships :film/vehicles]}
     (swapi-data :film {:id (str id)})))

#?(:clj
   (pco/defresolver all-species-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-species [:specie/id :specie/name :specie/average_height :specie/average_lifespan
                                         :specie/classification :specie/designation :specie/eye_colors :specie/films
                                         :specie/hair_colors :specie/homeworld :specie/language :specie/people
                                         :specie/skin_colors]}]}
     (let [{:keys [search page]} query-params
           opts (cond-> {}
                        search (assoc :search search)
                        page (assoc :page page))]
       {:swapi/all-species (swapi-data :species opts)})))

#?(:clj
   (pco/defresolver species-resolver [env {:specie/keys [id] :as params}]
     {::pco/output [:specie/id :specie/name :specie/average_height :specie/average_lifespan
                    :specie/classification :specie/designation :specie/eye_colors :specie/films
                    :specie/hair_colors :specie/homeworld :specie/language :specie/people
                    :specie/skin_colors]}
     (swapi-data :specie {:id (str id)})))

#?(:clj
   (pco/defresolver all-planets-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-planets [:planet/id :planet/name :planet/climate :planet/gravity
                                         :planet/diameter :planet/orbital_period :planet/population
                                         :planet/rotation_period :planet/terrain]}]}
     (let [{:keys [search page]} query-params
           opts (cond-> {}
                        search (assoc :search search)
                        page (assoc :page (str page)))]
       {:swapi/all-planets (swapi-data :planets opts)})))

#?(:clj
   (pco/defresolver planet-resolver [env {:planet/keys [id] :as params}]
     {::pco/output [:planet/id :planet/name :planet/climate :planet/gravity
                    :planet/diameter :planet/orbital_period :planet/population
                    :planet/rotation_period :planet/terrain]}
     (swapi-data :planet {:id (str id)})))

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
   (pco/defresolver all-entities-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-entities [:entity/id :entity/name :entity/type :entity/entity]}]}
     (let [operations [:people :vehicles :planets :films :species :starships]
           {:keys [search page]} query-params
           opts (cond-> {}
                        search (assoc :search search)
                        page (assoc :page (str page)))
           all (->> (mapcat #(swapi-data % opts) operations)
                    (remove nil?)
                    doall)]
       (tap> {:from ::all-entities-resolver :env env :params params :opts opts :all all})
       {:swapi/all-entities (mapv (fn [m]
                                    (when-let [kw-ns (namespace (first (keys m)))]
                                      (let [id (or (m (keyword kw-ns "id") :none))
                                            n (or (m (keyword kw-ns "name")) :none)
                                            entitytype (keyword kw-ns)]
                                        (when entitytype
                                              (let [id (cond
                                                         (not= :none id) (format "%s-%s" kw-ns id)
                                                         :else (new-uuid))]
                                                #:entity{:id id :name n :type entitytype #_#_:entity m}))))) all)})))

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
