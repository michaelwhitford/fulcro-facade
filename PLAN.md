# PLAN.md

Active and completed feature plans.

---

# Martian Client Exploration

## Overview

Enable AI to explore martian HTTP clients via REPL to discover available API operations, parameters, and response schemas.

## Status: ✅ COMPLETE

### All Phases Complete
- [x] Phase 1: Document `martian/explore` for operation discovery
- [x] Phase 2: Document `martian/response-for` for request execution
- [x] Phase 3: Create operation reference tables for SWAPI (12) and HPAPI (2)
- [x] Phase 4: Document tap> debugging (user receives via shadow-cljs preload)

---

## Files

| File | Purpose |
|------|---------|
| `MARTIAN.md` | Full documentation for martian client exploration |
| `components/swapi.clj` | SWAPI martian client (mount state) |
| `components/hpapi.clj` | HPAPI martian client (mount state) |
| `components/interceptors.clj` | Shared tap> interceptors |

---

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
