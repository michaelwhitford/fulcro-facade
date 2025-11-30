# Architecture

## Overview

Facade provides a fulcro-rad client for multiple backend APIs.

## APIs

### SWAPI (Star Wars)
- films, people, starships, vehicles, planets, species
- https://swapi.dev

### HPAPI (Harry Potter)  
- characters (students, staff, by house), spells
- https://hp-api.onrender.com

## Key Components

- `components/parser.clj` - Pathom3 parser with all resolvers
- `components/swapi.clj` - SWAPI HTTP client (martian)
- `components/hpapi.clj` - HP API HTTP client (martian)
- `model/*.cljc` - Resolvers and business logic
- `model_rad/*.cljc` - RAD attribute definitions
- `ui/*.cljc` - Fulcro components, forms, reports

## Configuration

- `config/defaults.edn` - Base config
- `config/dev.edn` - Development overrides
- Hierarchical merging with hot-reload support

## App State

- `us.whitford.facade.application/SPA` - App atom reference
- State atom: `(::app/state-atom @SPA)`

## Diagnostic Queries

```clojure
(us.whitford.facade.components.parser/parser {} [:radar/overview])
(us.whitford.facade.components.parser/parser {} [:radar/pathom-env])
```
