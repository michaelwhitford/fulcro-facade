(ns us.whitford.facade.model.entity
  "Universal search resolver that combines results from all backend APIs.
   
   DO NOT require a RAD model file in this ns. This ns is meant to be an ultimate
   leaf of the requires. Only include library code."
  (:require
   [clojure.string :as str]
   #?(:clj [com.wsscode.pathom3.connect.operation :as pco])
   #?(:clj [taoensso.timbre :as log])
   #?@(:clj [[us.whitford.facade.model.swapi :as swapi]
             [us.whitford.facade.model.hpapi :as hpapi]])))

(defn swapi-entity->unified
  "Transform a single SWAPI entity map to unified entity format.
   Pure function for transformation logic."
  [m]
  (when (and (map? m) (seq m))
    (when-let [first-key (first (keys m))]
      (when-let [kw-ns (and (keyword? first-key) (namespace first-key))]
        (let [entity-id (m (keyword kw-ns "id"))
              ;; Handle films which use :title instead of :name
              entity-name (or (m (keyword kw-ns "name"))
                              (m (keyword kw-ns "title"))
                              "Unknown")
              entity-type (keyword kw-ns)]
          (when (and entity-id entity-type)
            {:entity/id (str kw-ns "-" entity-id)
             :entity/name entity-name
             :entity/type entity-type}))))))

(defn hpapi-character->unified
  "Transform a HP character map to unified entity format.
   Pure function for transformation logic."
  [c]
  (when-let [id (:character/id c)]
    {:entity/id (str "character-" id)
     :entity/name (:character/name c)
     :entity/type :character}))

(defn hpapi-spell->unified
  "Transform a HP spell map to unified entity format.
   Pure function for transformation logic."
  [s]
  (when-let [id (:spell/id s)]
    {:entity/id (str "spell-" id)
     :entity/name (:spell/name s)
     :entity/type :spell}))

#?(:clj
   (defn fetch-swapi-entities
     "Fetch entities from SWAPI and transform to unified format."
     [search-term]
     (try
       (let [all-entities (swapi/fetch-and-transform-entities search-term)]
         (keep swapi-entity->unified all-entities))
       (catch Exception e
         (log/warn e "Failed to fetch SWAPI entities")
         []))))

#?(:clj
   (defn fetch-hpapi-entities
     "Fetch entities from Harry Potter API and transform to unified format."
     [search-term]
     (let [search-opts (when (and search-term (not (str/blank? search-term)))
                         {:search search-term})
           ;; Fetch characters and spells in parallel
           chars-future (future
                          (try
                            (when-let [chars (hpapi/hpapi-data :characters (or search-opts {}))]
                              (keep hpapi-character->unified chars))
                            (catch Exception e
                              (log/warn e "Failed to fetch HP characters")
                              [])))
           spells-future (future
                           (try
                             (when-let [spells (hpapi/hpapi-data :spells (or search-opts {}))]
                               (keep hpapi-spell->unified spells))
                             (catch Exception e
                               (log/warn e "Failed to fetch HP spells")
                               [])))]
       (concat (or @chars-future []) (or @spells-future [])))))

#?(:clj
   (defn fetch-all-entities
     "Fetch entities from all APIs in parallel and combine results."
     [search-term]
     (let [swapi-future (future (fetch-swapi-entities search-term))
           hpapi-future (future (fetch-hpapi-entities search-term))]
       (concat @swapi-future @hpapi-future))))

#?(:clj
   (pco/defresolver all-entities-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:swapi/all-entities [:entity/id :entity/name :entity/type]}]}
     (try
       (let [search-term (:search-term query-params)]
         ;; Don't fetch all entities when no search term - would be 570+ results
         (if (or (nil? search-term) (str/blank? search-term))
           (do
             (log/info "Universal search skipped - no search term provided")
             {:swapi/all-entities []})
           (let [all-entities (fetch-all-entities search-term)]
             (log/info "Universal search completed" {:search-term search-term :result-count (count all-entities)})
             {:swapi/all-entities (vec all-entities)})))
       (catch Exception e
         (log/error e "Failed to resolve all-entities search")
         {:swapi/all-entities []}))))

#?(:clj (def resolvers [all-entities-resolver]))
