# PLAN.md

Active and completed feature plans.

---

# Weather API Integration (wttr.in)

## Overview

Weather forecast integration using wttr.in API. Provides current conditions and 3-day forecast.

## Status: ✅ COMPLETE

### All Phases Complete
- [x] Phase 1: OpenAPI spec created (`wttr.yml`)
- [x] Phase 2: Martian client component (`components/wttr.clj`)
- [x] Phase 3: Model with resolver (`model/wttr.cljc`)
- [x] Phase 4: RAD attributes (`model_rad/wttr.cljc`)
- [x] Phase 5: UI components (`ui/wttr_forms.cljc`)
- [x] Phase 6: Menu integration in root.cljc and client.cljs routes
- [x] Phase 7: IP-based location via Pathom graph (bridge resolver)
- [x] Phase 8: "Use My Location" button using ipify.org + ip-api + wttr.in

---

## Files Created

| File | Purpose |
|------|---------|
| `src/main/wttr.yml` | OpenAPI 3.0 spec for wttr.in |
| `components/wttr.clj` | Martian HTTP client |
| `model/wttr.cljc` | Resolver and data transformations |
| `model_rad/wttr.cljc` | RAD attribute definitions |
| `ui/wttr_forms.cljc` | Weather lookup widget and forms |

## Files Modified

| File | Changes |
|------|---------|
| `config/defaults.edn` | Added wttr config |
| `model_rad/attributes.cljc` | Registered wttr attributes |
| `components/parser.clj` | Added wttr resolvers |
| `ui/root.cljc` | Added Weather menu |
| `client.cljs` | Added routes for weather components |

---

## Data Model

### Weather Entity
- `:weather/id` - Location string (identity)
- Current conditions: temp, feels-like, humidity, wind, pressure, etc.
- Location info: area-name, country, region, lat/lon
- Astronomy: sunrise, sunset, moon phase

### Weather Day Entity
- `:weather-day/date` - Date string (identity)
- Forecast: max/min/avg temps, sun hours, UV index
- Astronomy data embedded

---

## Pathom Graph: IP → Location → Weather

The bridge resolver `weather-from-ip-resolver` connects IP geolocation to weather:

```
User's IP → ipify.org (client) → ip-info-resolver → :ip-info/city 
                                                          ↓
                                            weather-from-ip-resolver
                                                          ↓
                                                   weather data
```

Query weather via IP - Pathom connects automatically:
```clojure
(parser {} [{[:ip-info/id "68.2.71.58"] 
             [:ip-info/city :weather/temp-c :weather/description]}])
;; => {[:ip-info/id "68.2.71.58"] 
;;     {:ip-info/city "Phoenix", :weather/temp-c "17", :weather/description "Clear"}}
```

---

## REPL Testing

```clojure
(require '[us.whitford.facade.components.wttr :refer [wttr-martian]])
(require '[martian.core :as martian])

(martian/explore wttr-martian)
;; => [[:forecast "Get weather forecast for a location"]]

@(martian/response-for wttr-martian :forecast {:location "London" :format "j1"})

;; Test via parser
(require '[us.whitford.facade.components.parser :refer [parser]])
(parser {} [{[:weather/id "Tokyo"] [:weather/temp-c :weather/description]}])

;; Test IP -> Weather graph connection
(parser {} [{[:ip-info/id "8.8.8.8"] 
             [:ip-info/city :weather/temp-c :weather/description]}])
```

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
