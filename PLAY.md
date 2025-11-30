# PLAY.md - Exploration Findings

## Summary

Facade is a well-structured Fulcro RAD application that provides a unified client for SWAPI (Star Wars API) and HPAPI (Harry Potter API). The app demonstrates good use of Fulcro's architecture including statecharts for routing, Pathom3 for resolvers, and RAD for forms/reports.

## Project Statistics

| Component | Count |
|-----------|-------|
| Mount States | 11 |
| RAD Attributes | 94 |
| SWAPI Resolvers | 13 |
| HPAPI Resolvers | 4 |
| Forms | 10 |
| Reports | 10 |
| Entity Types | 12 |

## What's Working Well

### 1. Clean Architecture
- Clear separation between model (`model/`), RAD definitions (`model_rad/`), and UI (`ui/`)
- Mount states properly manage component lifecycle
- Configuration is centralized and hierarchical (`config/defaults.edn`, `config/dev.edn`)

### 2. Pathom3 Integration
- Parser is well-configured with proper middleware stacking
- RADAR diagnostic tool provides excellent visibility into the system
- Query params are normalized for easier resolver access

### 3. Martian HTTP Client
- Swagger/OpenAPI specs drive API client generation (`swapi.yml`, `hpapi.yml`)
- Interceptors allow tap> debugging of requests/responses

### 4. Statecharts Routing
- Modern statechart-based routing with `ui-routes`
- Route change protection for unsaved forms
- Good integration with RAD forms/reports

### 5. Testing Approach
- Configuration tests validate structure without starting full system
- fulcro-spec style assertions are clean

## Recommendations for Improvement

### 1. **Reduce Resolver Duplication (High Impact)**

The SWAPI resolvers (`model/swapi.cljc`) contain significant duplication. All 6 list resolvers follow the same pattern:

```clojure
;; Current: Each resolver is ~20 lines with identical structure
(pco/defresolver all-people-resolver ...)
(pco/defresolver all-vehicles-resolver ...)
(pco/defresolver all-starships-resolver ...)
;; etc.
```

**Suggestion**: Create a resolver factory:

```clojure
(defn make-list-resolver [entity-key output-key output-spec]
  (pco/resolver
    {::pco/output [{output-key [:total {:results output-spec}]}]}
    (fn [{:keys [query-params]} _]
      (let [{:keys [search]} query-params
            opts (build-opts search query-params)
            {:keys [results total-count]} (swapi-data-paginated entity-key opts)]
        {output-key {:results (or results []) :total (or total-count 0)}}))))

(def all-people-resolver (make-list-resolver :people :swapi/all-people person-output-spec))
(def all-vehicles-resolver (make-list-resolver :vehicles :swapi/all-vehicles vehicle-output-spec))
```

This could reduce ~150 lines to ~50 lines while making the pattern explicit.

### 2. **Extract Report Control Boilerplate (Medium Impact)**

Every report in `swapi_forms.cljc` repeats identical pagination controls:

```clojure
ro/controls {::refresh ...
             ::prior-page ...
             ::next-page ...
             ::page-info ...}
ro/control-layout {:action-buttons [::refresh ::prior-page ::page-info ::next-page]}
```

**Suggestion**: Create shared control definitions:

```clojure
(def pagination-controls {...})
(def pagination-layout {...})

;; Then in each report:
ro/controls (merge pagination-controls {...specific...})
ro/control-layout pagination-layout
```

### 3. **Simplify URL->ID Transformation (Low Impact)**

The `transform-swapi` function chains 9 nearly identical `mapv` calls:

```clojure
(defn transform-swapi [input]
  (->> (mapv swapi-id input)
       (mapv (fn [m] (update-in-contains m [:films] ...)))
       (mapv (fn [m] (update-in-contains m [:starships] ...)))
       ;; ... 7 more
```

**Suggestion**: Use a single reduce with a list of keys to transform:

```clojure
(def url-fields #{:films :starships :species :vehicles :residents 
                  :people :characters :pilots :planets :homeworld})

(defn transform-swapi [input]
  (->> input
       (mapv swapi-id)
       (mapv (fn [m]
               (reduce (fn [acc k]
                         (if-let [v (get acc k)]
                           (update acc k #(if (coll? %) 
                                            (mapv swapiurl->id %) 
                                            (swapiurl->id %)))
                           acc))
                       m url-fields)))))
```

### 4. **Add More Test Coverage (Medium Impact)**

Current tests only cover configuration. Consider adding:
- Resolver unit tests (mock API responses)
- URL transformation tests
- Form state machine tests

### 5. **Consider Caching for Read-Only APIs**

Since SWAPI and HPAPI data rarely changes, consider:
- Adding `atom`-based caching for API responses
- TTL-based cache invalidation
- This would reduce API calls and improve UX

### 6. **Remove Commented/Dead Code**

Several files have commented-out code blocks that could be cleaned up:
- `swapi.clj`: Commented `image-encoder` setup
- `hpapi.clj`: Same pattern
- `swapi_forms.cljc`: Multiple commented-out mutations

### 7. **Standardize Naming Conventions**

Entity naming is inconsistent:
- SWAPI uses `:specie/id` (singular) for species entities
- Some attributes use underscores (`:person/birth_year`) from API response
- Consider using kebab-case consistently or documenting the convention

### 8. **Extract Martian Setup Pattern**

Both `swapi.clj` and `hpapi.clj` have nearly identical structure. Could be:

```clojure
(defn make-martian-state [config-key]
  (defstate name
    :start
    (let [{:keys [swagger-file server-url]} (get config config-key)]
      (martian-http/bootstrap-openapi swagger-file {...}))))
```

## Interesting Patterns Observed

### RADAR Diagnostic System
The fulcro-radar integration provides excellent introspection:

```clojure
(parser {} [:radar/overview])
;; Returns mount states, forms, reports, entities, attributes, references
```

This is a great debugging aid during development.

### Query Param Normalization
Smart middleware that allows resolvers to use simple keywords even when RAD sends namespaced params:

```clojure
(defn normalize-query-params [query-params]
  (reduce-kv (fn [m k v]
               (let [simple-key (keyword (name k))]
                 (cond-> m
                   (not (contains? m simple-key)) (assoc simple-key v))))
    query-params query-params))
```

## Files Explored

- `deps.edn` - Dependencies and aliases
- `shadow-cljs.edn` - ClojureScript build config  
- `src/dev/development.clj` - Development utilities
- `src/main/us/whitford/facade/components/*.clj` - Server components
- `src/main/us/whitford/facade/model/*.cljc` - Resolvers and business logic
- `src/main/us/whitford/facade/model_rad/*.cljc` - RAD attribute definitions
- `src/main/us/whitford/facade/ui/*.cljc` - UI components
- `src/main/config/*.edn` - Configuration files
- `src/test/**` - Test files

## REPL Experiments

Tested successfully:
- `(parser {} [:radar/overview])` - Full diagnostic output
- `(parser {} [{:swapi/all-people [:total {:results [:person/id :person/name]}]}])` - 82 people, 10 per page
- `(parser {} [{:hpapi/all-characters [:character/id :character/name :character/house]}])` - 400+ HP characters
- `(martian/explore swapi-martian)` - 12 SWAPI endpoints
- `(martian/explore hpapi-martian)` - 2 HPAPI endpoints

## Conclusion

Facade is a solid demonstration of Fulcro RAD capabilities. The main opportunities for simplification lie in:
1. Reducing boilerplate through factory functions and shared definitions
2. Adding test coverage for core business logic
3. Cleaning up dead code

The architecture is sound and the code is readable - improvements would primarily be about DRY principles and maintainability.
