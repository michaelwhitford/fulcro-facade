# Pathom3 Resolver Guide

> **Purpose**: This document explains Pathom3 concepts and resolver patterns used in this project.  
> **Audience**: AI agents and developers new to Pathom3.  
> **Scope**: Pathom3 resolver system. See FULCRO-RAD.md for RAD features.

---

## Table of Contents

1. [What is Pathom?](#what-is-pathom)
2. [Resolver Anatomy](#resolver-anatomy)
3. [Input and Output](#input-and-output)
4. [Resolver Types](#resolver-types)
5. [Query Planning](#query-planning)
6. [Error Handling](#error-handling)
7. [Common Patterns](#common-patterns)
8. [Debugging Resolvers](#debugging-resolvers)
9. [Troubleshooting](#troubleshooting)

---

## What is Pathom?

**Pathom** is a graph query engine that automatically resolves EQL (EDN Query Language) queries by:
- Matching queries to available resolvers
- Building an optimal execution plan
- Handling data dependencies automatically
- Providing error boundaries and fallbacks

### Why Pathom?

**Without Pathom**:
```clojure
;; Manual data fetching - you figure out dependencies
(defn fetch-person-with-homeworld [id]
  (let [person (fetch-person id)
        homeworld-id (:homeworld-id person)
        homeworld (fetch-planet homeworld-id)]
    (assoc person :homeworld homeworld)))
```

**With Pathom**:
```clojure
;; Declare what you can provide - Pathom figures out the plan
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/output [:person/name :person/homeworld-id]}
  (fetch-person id))

(pco/defresolver planet-resolver [{:planet/keys [id]}]
  {::pco/output [:planet/name]}
  (fetch-planet id))

;; Query asks for person with homeworld name
;; Pathom automatically:
;; 1. Calls person-resolver to get homeworld-id
;; 2. Calls planet-resolver to get planet name
;; 3. Returns complete data
```

### Core Concepts

- **Resolver** - Function that provides data
- **Input** - What data a resolver needs to run
- **Output** - What data a resolver provides
- **Query Plan** - Pathom's execution strategy
- **Environment** - Context passed to all resolvers

---

## Resolver Anatomy

### Basic Resolver

```clojure
(pco/defresolver person-name-resolver 
  [{:person/keys [id]}]                          ; ⭐ Destructure input
  {::pco/input  [:person/id]                     ; ⭐ Declare what you need
   ::pco/output [:person/name :person/height]}   ; ⭐ Declare what you provide
  
  ;; Resolver body - return a map
  {:person/name "Luke Skywalker"
   :person/height "172"})
```

### With Environment

```clojure
(pco/defresolver person-resolver 
  [env {:person/keys [id]}]                      ; ⭐ env is first param
  {::pco/output [:person/name]}
  
  (let [db (:database env)]                      ; ⭐ Access env data
    {:person/name (fetch-from-db db id)}))
```

### With Query Params

```clojure
(pco/defresolver all-people-resolver 
  [{:keys [query-params]} params]                ; ⭐ query-params from env
  {::pco/output [{:swapi/all-people [:person/id :person/name]}]}
  
  (let [search (:search-term query-params)]      ; ⭐ Get params
    {:swapi/all-people (search-people search)}))
```

---

## Input and Output

The heart of Pathom's power is the **input/output declaration**. This tells Pathom how to build query plans.

### How Pathom Uses Input/Output

```clojure
;; Resolver 1: Needs nothing, provides person list
(pco/defresolver all-people [env params]
  {::pco/input  []                               ; ⭐ No requirements
   ::pco/output [{:swapi/all-people [:person/id]}]}
  {:swapi/all-people [{:person/id "1"} {:person/id "2"}]})

;; Resolver 2: Needs person ID, provides details
(pco/defresolver person-details [{:person/keys [id]}]
  {::pco/input  [:person/id]                     ; ⭐ Requires this
   ::pco/output [:person/name :person/height]}
  {:person/name "Luke" :person/height "172"})
```

**Query**: `[{:swapi/all-people [:person/name]}]`

**Pathom's Query Plan**:
1. See `:swapi/all-people` requested
2. Find `all-people` resolver (no input needed) → run it
3. Get list with `:person/id` for each person
4. See `:person/name` requested for each person
5. Find `person-details` resolver (needs `:person/id`) → run for each
6. Combine results → return complete data

### Input Types

#### No Input (Root Resolver)
```clojure
{::pco/input []}  ; or omit entirely
```
**Use for**: Entry points, global data, collections

#### Single Attribute Input
```clojure
{::pco/input [:person/id]}
```
**Use for**: Fetching entity by ID

#### Multiple Attribute Input
```clojure
{::pco/input [:person/id :person/type]}
```
**Use for**: Need multiple pieces of data to resolve

#### Optional Input
```clojure
{::pco/input [:person/id (pco/? :person/cache-key)]}
```
**Use for**: Performance hints, optional context

### Output Types

#### Simple Attributes
```clojure
{::pco/output [:person/name :person/height]}
```

#### Nested Data
```clojure
{::pco/output [:person/name 
               {:person/homeworld [:planet/name]}]}
```

#### Collections
```clojure
{::pco/output [{:swapi/all-people [:person/id :person/name]}]}
```

---

## Resolver Types

### 1. Root Resolvers

**Entry points** to the graph. No input required.

```clojure
(pco/defresolver all-films [env params]
  {::pco/output [{:swapi/all-films [:film/id :film/title]}]}
  
  {:swapi/all-films (fetch-all-films)})
```

**Use for**:
- Collections (all-X)
- Global data (current-user, config)
- Entry points for queries

### 2. Entity Resolvers

**Fetch entities by ID**. Require identity attribute.

```clojure
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/input  [:person/id]
   ::pco/output [:person/name :person/height :person/mass]}
  
  (fetch-person id))
```

**Use for**:
- Loading entity details
- Expanding references
- Following relationships

### 3. Derived Resolvers

**Compute from other data**. Take data, return computed data.

```clojure
(pco/defresolver person-bmi [{:person/keys [height mass]}]
  {::pco/input  [:person/height :person/mass]
   ::pco/output [:person/bmi]}
  
  {:person/bmi (/ mass (* height height))})
```

**Use for**:
- Computed fields
- Format conversions
- Data transformations

### 4. Batch Resolvers

**Optimize N+1 queries**. Fetch multiple entities at once.

```clojure
(pco/defresolver people-batch-resolver [env people]
  {::pco/input  [:person/id]
   ::pco/output [:person/name]
   ::pco/batch? true}                            ; ⭐ Enable batching
  
  (let [ids (map :person/id people)
        results (fetch-people-batch ids)]
    (map-indexed (fn [idx result]
                   {:person/name (:name result)})
                 results)))
```

**Use for**:
- Fetching many entities from same source
- Reducing API calls
- Performance optimization

### 5. Mutations

**Change data**. Return updated data or confirmation.

```clojure
(pco/defmutation create-person [env {:person/keys [name height]}]
  {::pco/output [:person/id :person/name]}
  
  (let [id (save-person! {:name name :height height})]
    {:person/id id :person/name name}))
```

**Use for**:
- Creating entities
- Updating entities
- Deleting entities
- Side effects

---

## Query Planning

Pathom builds an **execution plan** based on your query and available resolvers.

### Example: Query Planning

**Available Resolvers**:
```clojure
;; Root resolver
(pco/defresolver all-people []
  {::pco/output [{:swapi/all-people [:person/id]}]}
  ...)

;; Entity resolver
(pco/defresolver person-details [{:person/keys [id]}]
  {::pco/input [:person/id]
   ::pco/output [:person/name :person/height]}
  ...)

;; Derived resolver
(pco/defresolver person-homeworld [{:person/keys [homeworld-id]}]
  {::pco/input [:person/homeworld-id]
   ::pco/output [{:person/homeworld [:planet/name]}]}
  ...)
```

**Query**: 
```clojure
[{:swapi/all-people [:person/name {:person/homeworld [:planet/name]}]}]
```

**Pathom's Plan**:
```
1. Root: all-people → provides [:person/id]
2. For each person:
   a. person-details (has :person/id) → provides [:person/name :person/homeworld-id]
   b. person-homeworld (has :person/homeworld-id) → provides [:person/homeworld]
3. Return combined results
```

### Plan Optimization

Pathom automatically:
- **Eliminates unnecessary resolvers** (don't ask for :height if not in query)
- **Batches when possible** (fetch all people at once, not one by one)
- **Parallelizes independent resolvers** (fetch person and planet concurrently)
- **Caches results** (same entity ID fetched once)

### Viewing the Plan

```clojure
;; In this project, use RADAR
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))

;; Run query and check logs
(p {} [{:swapi/all-people [:person/name]}])

;; Pathom logs show which resolvers ran and in what order
```

---

## Error Handling

Pathom provides **error boundaries** - one resolver failing doesn't crash the whole query.

### Best Practices

#### Always Return a Map

```clojure
;; ❌ BAD - Exception bubbles up
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/output [:person/name]}
  (fetch-person id))  ; Might throw!

;; ✅ GOOD - Graceful degradation
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/output [:person/name]}
  (try
    (or (fetch-person id) {})          ; ⭐ Return empty map on nil
    (catch Exception e
      (log/error e "Failed to fetch person" {:id id})
      {})))                            ; ⭐ Return empty map on error
```

#### Log with Context

```clojure
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/output [:person/name]}
  (try
    (fetch-person id)
    (catch Exception e
      (log/error e "Person fetch failed" 
        {:person/id id                 ; ⭐ Include relevant data
         :error-type (class e)})
      {})))
```

#### Provide Defaults

```clojure
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/output [:person/name :person/status]}
  (try
    (fetch-person id)
    (catch Exception e
      {:person/status :error})))       ; ⭐ At least provide something useful
```

### Error Attributes

Pathom adds metadata about errors:

```clojure
;; Query result might include:
{:person/name "Luke"
 :com.wsscode.pathom3.error/cause :com.wsscode.pathom3.error/node-errors
 :com.wsscode.pathom3.error/node-error-details {...}}
```

---

## Common Patterns

### Pattern 1: Collection with Search

**Report that filters based on search term**:

```clojure
(pco/defresolver all-people-resolver 
  [{:keys [query-params]} params]
  {::pco/output [{:swapi/all-people [:person/id :person/name]}]}
  
  (try
    (let [search-term (:search-term query-params)      ; ⭐ Get from params
          results (if (str/blank? search-term)
                    []                                  ; ⭐ Empty search = no results
                    (search-people search-term))]
      {:swapi/all-people results})
    (catch Exception e
      (log/error e "Search failed" {:search-term (:search-term query-params)})
      {:swapi/all-people []})))
```

**How RAD passes params**: See FULCRO-RAD.md section on `ro/load-options`

### Pattern 2: Entity by ID

**Fetch single entity details**:

```clojure
(pco/defresolver person-resolver 
  [env {:person/keys [id]}]
  {::pco/input  [:person/id]
   ::pco/output [:person/name :person/height :person/mass]}
  
  (try
    (let [person (fetch-person id)]
      (or person {}))                              ; ⭐ Handle not found
    (catch Exception e
      (log/error e "Person fetch failed" {:id id})
      {})))
```

### Pattern 3: Reference Expansion

**Automatically expand foreign keys**:

```clojure
;; Person has homeworld-id (string)
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/output [:person/name :person/homeworld-id]}
  {:person/name "Luke" :person/homeworld-id "1"})

;; Expand homeworld-id to full planet
(pco/defresolver person-homeworld-resolver 
  [{:person/keys [homeworld-id]}]
  {::pco/input  [:person/homeworld-id]                ; ⭐ Needs the ID
   ::pco/output [{:person/homeworld [:planet/id]}]}   ; ⭐ Provides full planet
  
  {:person/homeworld {:planet/id homeworld-id}})      ; ⭐ Return as ident

;; Now queries can ask for {:person/homeworld [:planet/name]}
;; Pathom will automatically fetch planet details
```

### Pattern 4: Parallel API Calls

**Fetch from multiple sources and merge**:

```clojure
(pco/defresolver universal-search-resolver
  [{:keys [query-params]} params]
  {::pco/output [{:app/search-results [:entity/id :entity/name :entity/type]}]}
  
  (try
    (let [search-term (:search-term query-params)
          
          ;; ⭐ Parallel fetch using future/deref
          swapi-future (future (search-swapi search-term))
          hp-future (future (search-hp search-term))
          
          swapi-results @swapi-future
          hp-results @hp-future
          
          combined (concat swapi-results hp-results)]
      
      {:app/search-results combined})
    (catch Exception e
      {:app/search-results []})))
```

### Pattern 5: Transform External API Data

**Convert API response to Fulcro-friendly format**:

```clojure
(pco/defresolver all-planets-resolver [env params]
  {::pco/output [{:swapi/all-planets [:planet/id :planet/name]}]}
  
  (try
    (let [response @(martian/response-for swapi-client :planets)
          planets (:results (:body response))
          
          ;; ⭐ Transform: extract ID from URL, namespace keys
          transformed (mapv (fn [planet]
                              {:planet/id (extract-id (:url planet))
                               :planet/name (:name planet)})
                            planets)]
      
      {:swapi/all-planets transformed})
    (catch Exception e
      {:swapi/all-planets []})))
```

---

## Debugging Resolvers

### 1. Use RADAR to Inspect

```clojure
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))

;; See all available resolvers
(p {} [:radar/pathom-env])

;; Look at resolver details:
;; {:resolvers {:root [...] :entity [...] :derived [...]}}
```

### 2. Add Logging

```clojure
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/output [:person/name]}
  
  (log/info "person-resolver called" {:id id})    ; ⭐ Log entry
  
  (let [result (fetch-person id)]
    (log/info "person-resolver result" result)    ; ⭐ Log result
    result))
```

### 3. Use tap>

```clojure
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/output [:person/name]}
  
  (tap> {:resolver :person :input id})            ; ⭐ For REPL inspection
  
  (let [result (fetch-person id)]
    (tap> {:resolver :person :output result})
    result))
```

### 4. Check Pathom Logs

Pathom logs show:
- Which resolvers matched the query
- Execution order
- Errors and warnings
- Performance metrics

Look for `pathom3` in log output.

---

## Troubleshooting

### "Attribute Unreachable" Error

**Problem**: Query asks for attribute but Pathom can't find a resolver.

```clojure
;; Error: :person/name cannot be resolved
{:com.wsscode.pathom3.error/cause :com.wsscode.pathom3.error/attribute-unreachable}
```

**Common Causes**:

1. **No resolver provides it**:
   ```clojure
   ;; Missing :person/name in output
   (pco/defresolver person-resolver [{:person/keys [id]}]
     {::pco/output [:person/height]})  ; ❌ No :person/name
   ```

2. **Wrong namespace**:
   ```clojure
   ;; Query asks for :person/name
   ;; Resolver provides :people/name (wrong namespace)
   ```

3. **Missing input data**:
   ```clojure
   ;; Resolver needs :person/id
   {::pco/input [:person/id]}
   
   ;; But query doesn't provide it:
   [:person/name]  ; ❌ Where's the :person/id?
   ```

**Solution**:
- Check resolver `::pco/output` includes the attribute
- Verify namespace matches exactly
- Ensure query provides required `::pco/input` data

### Resolver Doesn't Run

**Problem**: Resolver defined but never executes.

**Common Causes**:

1. **Not registered**:
   ```clojure
   ;; Check components/parser.clj
   (def all-resolvers
     [person-resolver  ; ⭐ Must be in the list
      planet-resolver
      ...])
   ```

2. **Input not available**:
   ```clojure
   ;; Needs :person/id but it's not in the graph yet
   {::pco/input [:person/id]}
   ```

3. **Wrong namespace**:
   ```clojure
   ;; Resolver uses :people/id
   ;; Query uses :person/id
   ```

**Solution**:
- Verify resolver is in `all-resolvers` vector
- Check RADAR to see if resolver is registered
- Verify input requirements are met

### Resolver Returns Nil

**Problem**: Resolver runs but returns `nil` instead of a map.

```clojure
;; ❌ BAD
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/output [:person/name]}
  (fetch-person id))  ; Returns nil if not found
```

**Solution**: Always return a map:
```clojure
;; ✅ GOOD
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/output [:person/name]}
  (or (fetch-person id) {}))  ; ⭐ Return empty map on nil
```

### N+1 Query Problem

**Problem**: Fetching 100 people makes 100 API calls.

```clojure
;; This runs once per person:
(pco/defresolver person-details [{:person/keys [id]}]
  {::pco/input [:person/id]
   ::pco/output [:person/name]}
  (fetch-person id))  ; ❌ Called 100 times!
```

**Solution**: Use batch resolver:
```clojure
(pco/defresolver person-details-batch [env people]
  {::pco/input [:person/id]
   ::pco/output [:person/name]
   ::pco/batch? true}            ; ⭐ Enable batching
  
  (let [ids (map :person/id people)
        results (fetch-people-batch ids)]  ; ⭐ One call for all
    results))
```

---

## Next Steps

- See **FULCRO-RAD.md** for how RAD reports/forms use Pathom resolvers
- See **FULCRO.md** for understanding Fulcro queries (EQL)
- See **QUICK_REFERENCE.md** for project-specific resolver examples
- See **RADAR.md** for runtime resolver introspection

---

## Further Reading

- [Pathom3 Documentation](https://pathom3.wsscode.com/)
- [EQL Specification](https://edn-query-language.org/)
- [Pathom Tutorials](https://pathom3.wsscode.com/docs/tutorials/)
