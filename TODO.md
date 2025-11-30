# TODO.md

Potential improvements and tech debt.

---

## High Impact

### Reduce Resolver Duplication

The SWAPI resolvers (`model/swapi.cljc`) contain significant duplication. All 6 list resolvers follow the same pattern:

```clojure
;; Current: Each resolver is ~20 lines with identical structure
(pco/defresolver all-people-resolver ...)
(pco/defresolver all-vehicles-resolver ...)
(pco/defresolver all-starships-resolver ...)
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
```

This could reduce ~150 lines to ~50 lines.

---

## Medium Impact

### Extract Report Control Boilerplate

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
```

### Add Test Coverage

Current tests cover configuration and entity transforms. Consider adding:
- Resolver unit tests (mock API responses)
- URL transformation tests
- Form state machine tests

### Consider Caching for Read-Only APIs

Since SWAPI and HPAPI data rarely changes:
- Add `atom`-based caching for API responses
- TTL-based cache invalidation
- Would reduce API calls and improve UX

---

## Low Impact

### Simplify URL->ID Transformation

The `transform-swapi` function chains 9 nearly identical `mapv` calls.

**Suggestion**: Use a single reduce with a list of keys:

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

### Remove Commented/Dead Code

Several files have commented-out code blocks:
- `swapi_forms.cljc`: Multiple commented-out mutations

### Implement Image Encoder for Martian Clients

The `image-encoder` in `swapi.clj` and `hpapi.clj` is stubbed out but not wired up. Could be useful for:
- Fetching character/entity images directly through martian
- Binary response handling for image/* content types
- Custom encode/decode for image payloads

### Standardize Naming Conventions

Entity naming is inconsistent:
- SWAPI uses `:specie/id` (singular) for species entities
- Some attributes use underscores (`:person/birth_year`) from API response
- Consider kebab-case consistently or documenting the convention
