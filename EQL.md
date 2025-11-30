# EQL Query Patterns for Facade

## Working Examples (copy-paste ready)

### Entity by ID
```clj
(p {} [{[:person/id "1"] [:person/name :person/birth_year :person/eye_color]}])
(p {} [{[:film/id "1"] [:film/title :film/director :film/release_date]}])
(p {} [{[:planet/id "1"] [:planet/name :planet/climate :planet/terrain]}])
```

### Collections - SWAPI (paginated wrapper)
```clj
(p {} [{:swapi/all-people [:total {:results [:person/name :person/birth_year]}]}])
(p {} [{:swapi/all-films [:total {:results [:film/title :film/director]}]}])
(p {} [{:swapi/all-planets [:total {:results [:planet/name :planet/climate]}]}])
(p {} [{:swapi/all-starships [:total {:results [:starship/name :starship/model]}]}])
(p {} [{:swapi/all-vehicles [:total {:results [:vehicle/name :vehicle/model]}]}])
(p {} [{:swapi/all-species [:total {:results [:specie/name :specie/language]}]}])
```

### Collections - HPAPI (flat arrays)
```clj
(p {} [{:hpapi/all-characters [:character/name :character/house :character/species]}])
(p {} [{:hpapi/all-spells [:spell/name :spell/description]}])
```

### Collections - Other
```clj
(p {} [{:ipapi/all-ip-lookups [:ip-info/query :ip-info/country :ip-info/city]}])
(p {} [{:account/all-accounts [:account/email :account/active?]}])
```

### Unified Search (cross-API)
```clj
(p {} [{:swapi/all-entities [:entity/id :entity/name :entity/type]}])
```

---

## EQL Syntax Reference

### Ident (entity lookup)
```clj
[[:person/id "1"]]                              ; just the ident
[{[:person/id "1"] [:person/name]}]             ; ident with subquery
```

### Ident with params
```clj
[([:person/id "1"] {:search "something"})]
```

### Join (collection)
```clj
[{:swapi/all-people [:person/name]}]            ; basic join
[{:swapi/all-people [:total {:results [...]}]}] ; nested join
```

### Join with params
```clj
[({:swapi/all-people [:person/name]} {:search "Luke"})]
```

### Ident join with params
```clj
[{([:person/id "1"] {:search "Luke"}) [:person/name]}]
```

### Wildcard (all fields)
```clj
[{[:person/id "1"] ['*]}]                       ; all fields for entity
```

---

## Quick Discovery

```clj
;; Setup
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))

;; Find available root resolvers (collection entry points)
(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :root 
     (map (juxt :name :output)))

;; Find entity resolvers (by-id lookups)  
(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :entity
     (map (juxt :name :input :output)))

;; Get queryable fields for an entity
(->> (p {} [:radar/overview]) :radar/overview :radar/entities
     (filter #(= "person" (:name %))) first :attributes)
```


