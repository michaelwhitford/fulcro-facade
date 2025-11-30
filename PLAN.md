# Universal Search Feature

## Overview

Universal search across SWAPI (Star Wars) and Harry Potter APIs from a single SearchReport.

## Status: ✅ COMPLETE

### All Phases Complete
- [x] Phase 1: Backend wiring - resolver accepts `:search-term` param
- [x] Phase 2: Header search form submits to SearchReport
- [x] Phase 3: Harry Potter integration - characters and spells included
- [x] Phase 4: Navigation support - UUID and numeric ID parsing for all 8 entity types
- [x] Phase 5: Empty state - returns empty when no search term (prevents 570+ entity load)
- [x] Phase 6: Tests - 627 assertions pass across 59 tests

---

## Architecture

### Entity Types (8 total)

| Type | Source | Icon | ID Format |
|------|--------|------|-----------|
| `:person` | SWAPI | user | `person-1` |
| `:film` | SWAPI | film | `film-4` |
| `:vehicle` | SWAPI | car | `vehicle-14` |
| `:starship` | SWAPI | space shuttle | `starship-10` |
| `:specie` | SWAPI | hand spock | `specie-3` |
| `:planet` | SWAPI | globe | `planet-1` |
| `:character` | HP | magic | `character-{uuid}` |
| `:spell` | HP | bolt | `spell-{uuid}` |

### Data Flow

```
Search component (header)
    ↓ set-search-term-and-run mutation
    ↓ uir/route-to! navigates to SearchReport
    ↓ report/run-report! triggers load
    
SearchReport 
    ↓ ro/load-options adds :search-term to params
    ↓ uism/load with params

all-entities-resolver (model/entity.cljc)
    ↓ reads :search-term from query-params
    ↓ parallel fetch from SWAPI + HP APIs
    ↓ transforms to unified {:entity/id :entity/name :entity/type}
    
SearchReport state machine
    ↓ :event/loaded triggers filter/sort/paginate
    ↓ populates :ui/current-rows
    ↓ renders via SearchResultRow BodyItem
```

---

## Key Implementation Details

### Control Parameter Flow (RAD Reports)

**Global controls** (`:local? false` or unspecified):
```clojure
;; Storage path
[::control/id <control-key> ::control/value]

;; Example
[:com.fulcrologic.rad.control/id ::search-term :com.fulcrologic.rad.control/value]
```

**Local controls** (`:local? true`):
```clojure
;; Storage path
(conj report-ident :ui/parameters <control-key>)
```

### Passing Parameters to Resolvers

Use `ro/load-options` to transform control params to resolver params:
```clojure
ro/load-options (fn [env]
                  (let [params (report/current-control-parameters env)
                        search-term (::search-term params)]
                    {:params (assoc params :search-term search-term)}))
```

### Component Ident Best Practice

Use keyword shorthand for simple idents:
```clojure
;; ✅ CORRECT
:ident :entity/id

;; ❌ WRONG - closure doesn't have access to props during normalization
:ident (fn [] [:entity/id (:entity/id props)])
```

### Triggering Report from External Component

```clojure
(defmutation set-search-term-and-run [{:keys [search-term]}]
  (action [{:keys [state app]}]
    ;; 1. Set control value at correct path
    (swap! state assoc-in [::control/id ::search-term ::control/value] search-term)
    ;; 2. Trigger report run after state update
    #?(:cljs (when app
               (js/setTimeout 
                 #(report/run-report! app SearchReport)
                 100)))))
```

---

## Files Changed

### New Files
| File | Purpose |
|------|---------|
| `model_rad/entity.cljc` | Entity RAD attributes |
| `model/entity.cljc` | Universal search resolver + transforms |
| `test/.../model/entity_test.cljc` | Transformation function tests |
| `test/.../ui/search_forms_test.cljc` | UI helper tests |

### Modified Files
| File | Changes |
|------|---------|
| `ui/search_forms.cljc` | Search component, SearchReport, SearchResultRow |
| `components/parser.clj` | Added entity resolvers |

---

## REPL Testing

```clojure
;; Test resolver directly
(require '[us.whitford.facade.components.parser :as parser])
(parser/parser {} ['({:swapi/all-entities [:entity/id :entity/name :entity/type]} 
                     {:search-term "harry"})])
;; => {:swapi/all-entities [{:entity/id "character-..." :entity/name "Harry Potter" :entity/type :character}]}
```

---

## Run Tests

```bash
clojure -M:run-tests
# 59 tests, 627 assertions, 0 failures
```
