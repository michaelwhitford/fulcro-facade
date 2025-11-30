# Architecture

## Overview

Facade is a Fulcro RAD application that serves as a client to multiple backend APIs, featuring universal search across all data sources.

## Project Statistics

| Component | Count |
|-----------|-------|
| Mount States | 15 |
| RAD Attributes | 163 |
| Forms | 12 |
| Reports | 11 |
| Entity Types | 16 |
| References | 15 |

Run `(-> (p {} [:radar/overview]) :radar/overview :radar/summary)` for current counts.

## APIs

### SWAPI (Star Wars)

- Films, People, Starships, Vehicles, Planets, Species
- https://swapi.dev

### HPAPI (Harry Potter)

- Characters (students, staff, by house), Spells
- https://hp-api.onrender.com

### IPAPI (IP Geolocation)

- IP address lookup, location, ISP info
- http://ip-api.com

### Wttr (Weather)

- Current weather, forecasts by location
- https://wttr.in

## Key Components

### Core Infrastructure

| File                             | Purpose                           |
| -------------------------------- | --------------------------------- |
| `components/parser.clj`          | Pathom3 parser with all resolvers |
| `components/config.clj`          | Mount-based configuration loading |
| `components/database.clj`        | Datomic connection management     |
| `components/server.clj`          | HTTP server (ring)                |
| `components/ring_middleware.clj` | Request/response middleware       |
| `components/statecharts.clj`     | Statechart routing                |

### API Clients

| File                   | Purpose                       |
| ---------------------- | ----------------------------- |
| `components/swapi.clj` | SWAPI HTTP client (martian)   |
| `components/hpapi.clj` | HP API HTTP client (martian)  |
| `components/ipapi.clj` | IP API HTTP client (martian)  |
| `components/wttr.clj`  | Weather HTTP client (martian) |

### RAD Infrastructure

| File                               | Purpose                     |
| ---------------------------------- | --------------------------- |
| `components/auto_resolvers.clj`    | Generated Datomic resolvers |
| `components/blob_store.clj`        | File upload storage         |
| `components/save_middleware.clj`   | Form save processing        |
| `components/delete_middleware.clj` | Entity deletion processing  |

### Application Layers

| Directory          | Purpose                           |
| ------------------ | --------------------------------- |
| `model/*.cljc`     | Resolvers and business logic      |
| `model_rad/*.cljc` | RAD attribute definitions         |
| `ui/*.cljc`        | Fulcro components, forms, reports |

## Configuration

- `config/defaults.edn` - Base configuration
- `config/dev.edn` - Development overrides
- `config/prod.edn` - Production overrides
- Hierarchical merging (defaults ← env-specific)

## Hot Reload

- **Client (CLJS)**: Hot-reloads automatically via shadow-cljs
- **Server (CLJ)**: Requires restart via `(development/restart)` from clj REPL

## App State

- `us.whitford.facade.application/SPA` - App atom reference
- State atom: `(::app/state-atom @SPA)`

## Query Param Normalization

The parser includes middleware that normalizes query-params, allowing resolvers to use simple keywords even when RAD sends namespaced params:

```clojure
;; RAD sends: {:us.whitford.facade.ui.search-forms/search-term "luke"}
;; Resolver receives both original AND: {:search-term "luke"}

;; So resolvers can destructure simply:
(let [{:keys [search-term]} query-params] ...)
```

See `components/parser.clj` for implementation.

## Diagnostic Queries

```clojure
(require '[us.whitford.facade.components.parser :as parser])

;; System overview
(parser/parser {} [:radar/overview])

;; Pathom environment (resolvers, mutations)
(parser/parser {} [:radar/pathom-env])
```

See `RADAR.md` for comprehensive introspection patterns.
