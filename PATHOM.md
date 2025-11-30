# Pathom3 Concepts

> **Purpose**: Understanding how Pathom resolvers work - the "why" behind the patterns.

---

## Mental Model

Pathom is a **graph query planner**. You declare what data resolvers can provide, and Pathom figures out how to satisfy queries.

```
┌─────────────────────────────────────────────────────────────┐
│                        EQL Query                             │
│  [{:swapi/all-people [:person/name :person/homeworld]}]     │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Query Planner                             │
│  "I need all-people, then person details, then homeworld"   │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                      Resolvers                               │
│  Each declares: "Give me X, I'll provide Y"                 │
└─────────────────────────────────────────────────────────────┘
```

**Key insight**: You don't write fetch logic. You declare capabilities. Pathom builds the plan.

---

## The Input/Output Contract

Every resolver declares what it **needs** (input) and what it **provides** (output):

```clojure
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/input  [:person/id]           ; "Give me a person ID..."
   ::pco/output [:person/name          ; "...I'll give you name, height, mass"
                 :person/height 
                 :person/mass]}
  (fetch-person id))
```

Pathom chains resolvers by matching outputs to inputs:

```
Query: [:person/name] for person "1"

Plan:
1. I have [:person/id "1"]
2. person-resolver needs :person/id, provides :person/name ✓
3. Run person-resolver → done
```

---

## Resolver Categories

### Root Resolvers (Entry Points)
No input required - these start query chains:

```clojure
(pco/defresolver all-people [env params]
  {::pco/output [{:swapi/all-people [:person/id]}]}
  {:swapi/all-people (fetch-all-people)})
```

### Entity Resolvers (By ID)
Require an identity, provide details:

```clojure
(pco/defresolver person-details [{:person/keys [id]}]
  {::pco/input  [:person/id]
   ::pco/output [:person/name :person/height]}
  (fetch-person id))
```

### Derived Resolvers (Computed)
Transform existing data:

```clojure
(pco/defresolver weather-from-city [{:keys [ip-info/city]}]
  {::pco/input  [:ip-info/city]
   ::pco/output [:weather/temp-c]}
  (fetch-weather-for city))
```

---

## Query Planning in Action

Given these resolvers:
- `all-people` → provides `[:person/id]`
- `person-details` → needs `:person/id`, provides `[:person/name :person/homeworld-id]`
- `planet-details` → needs `:planet/id`, provides `[:planet/name]`

Query: `[{:swapi/all-people [:person/name {:person/homeworld [:planet/name]}]}]`

Pathom's plan:
1. Run `all-people` → get list with `:person/id`
2. For each person, run `person-details` → get `:person/name`, `:person/homeworld-id`
3. For each homeworld, run `planet-details` → get `:planet/name`
4. Assemble and return

**You didn't write this orchestration** - Pathom derived it from input/output declarations.

---

## The Environment

Resolvers receive `env` - context for the request:

```clojure
(pco/defresolver search-resolver [{:keys [query-params]} params]
  {::pco/output [{:results [:entity/id]}]}
  (let [term (:search-term query-params)]  ; Access request params
    {:results (search term)}))
```

Common env keys:
- `:query-params` - Parameters passed with the query
- Custom keys injected by your parser setup

---

## Error Handling Philosophy

Pathom provides **error boundaries** - one resolver failing doesn't crash the query.

**Critical rule**: Always return a map, never throw:

```clojure
;; ❌ Throws on error - breaks entire query
(pco/defresolver bad-resolver [{:person/keys [id]}]
  {::pco/output [:person/name]}
  (fetch-person id))  ; might throw!

;; ✅ Graceful - returns empty map on failure
(pco/defresolver good-resolver [{:person/keys [id]}]
  {::pco/output [:person/name]}
  (try
    (or (fetch-person id) {})
    (catch Exception e
      (log/error e "Failed" {:id id})
      {})))
```

---

## Common Pitfalls

### 1. Returning Nil
Resolvers must return maps. `nil` breaks Pathom.

```clojure
;; ❌ Returns nil if not found
(fetch-person id)

;; ✅ Always a map
(or (fetch-person id) {})
```

### 2. Wrong Namespace
Query asks for `:person/name`, resolver provides `:people/name` - won't match.

### 3. Missing Input
Resolver needs `:person/id` but query doesn't provide a way to get it.

### 4. Not Registered
Resolver defined but not added to parser's resolver list.

---

## Discover Resolvers

```clj
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))

;; All resolvers by category
(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers)

;; Root resolvers (entry points)
(->> ... :resolvers :root (map (juxt :name :output)))

;; Entity resolvers (by-id)
(->> ... :resolvers :entity (map (juxt :name :input :output)))
```

---

## Further Reading

- [Pathom3 Documentation](https://pathom3.wsscode.com/)
- [EQL Specification](https://edn-query-language.org/)
