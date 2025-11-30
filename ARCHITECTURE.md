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

Run the following for current counts:
```clj
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))
(-> (p {} [:radar/overview]) :radar/overview :radar/summary)
```

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

| File                             | Purpose                               |
| -------------------------------- | ------------------------------------- |
| `components/parser.clj`          | Pathom3 parser with all resolvers     |
| `components/config.clj`          | Mount-based configuration loading     |
| `components/database.clj`        | Datomic connection management         |
| `components/server.clj`          | HTTP server (ring)                    |
| `components/ring_middleware.clj` | Request/response middleware           |
| `components/statecharts.clj`     | Statechart routing                    |
| `components/interceptors.clj`    | Shared martian interceptors (tap/log) |
| `components/utils.cljc`          | Shared utilities (encoding, parsing)  |

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

### Model Layer

| File                    | Purpose                                     |
| ----------------------- | ------------------------------------------- |
| `model/swapi.cljc`      | SWAPI resolvers and data transformation     |
| `model/hpapi.cljc`      | Harry Potter resolvers                      |
| `model/ipapi.cljc`      | IP geolocation resolvers                    |
| `model/wttr.cljc`       | Weather resolvers                           |
| `model/account.cljc`    | Account entity resolvers                    |
| `model/entity.cljc`     | Shared entity resolvers                     |
| `model/file.cljc`       | File upload handling                        |
| `model/agent_comms.cljc`| CLJ↔CLJS agent communication (inbox-based)  |
| `model/prompt.cljc`     | Statechart-based user prompts (ask!/answer) |

### RAD Attributes

| Directory          | Purpose                           |
| ------------------ | --------------------------------- |
| `model_rad/*.cljc` | RAD attribute definitions         |

### UI Layer

| File                   | Purpose                               |
| ---------------------- | ------------------------------------- |
| `ui/root.cljc`         | App root, routing, navigation         |
| `ui/*_forms.cljc`      | RAD forms and reports per API         |
| `ui/search_forms.cljc` | Universal search across all APIs      |
| `ui/toast.cljc`        | Toast notifications + prompt polling  |
| `ui/game.cljc`         | Interactive toast-based games         |

### Library

| File               | Purpose                       |
| ------------------ | ----------------------------- |
| `lib/logging.clj`  | Timbre logging configuration  |

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
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))

;; System overview
(p {} [:radar/overview])

;; Pathom environment (resolvers, mutations)
(p {} [:radar/pathom-env])
```

See `RADAR.md` for comprehensive introspection patterns.

## Agent Communication

Two mechanisms for CLJ REPL ↔ Browser communication:

### Toast Notifications (CLJS, fire-and-forget)

```clojure
;; From CLJS REPL - displays toast in browser
(require '[us.whitford.facade.ui.toast :refer [toast!]])
(toast! "Task complete! 🤖")
```

### Statechart Prompts (CLJ → Browser → CLJ)

```clojure
;; Ask a yes/no question from CLJ REPL
(require '[us.whitford.facade.model.prompt :as prompt])
(def q (prompt/ask! "Deploy to production?"))
;; => {:session-id :prompt/abc123 :status :awaiting-response}

;; Poll for answer (user sees toast in browser)
(prompt/get-result q)
;; => {:status :completed :answer true}
```

The prompt system uses statecharts for clean state management:
- States: `:ask/idle` → `:ask/pending` → `:ask/completed` | `:ask/timeout`
- Browser polls for pending questions and shows toast UI
- Answers flow back via Pathom mutation

### Agent Messages (CLJS → CLJ inbox)

```clojure
;; From CLJS - send message to CLJ inbox
(comp/transact! @SPA [(agent-comms/send-message {:message "hello" :data {:foo 1}})])

;; From CLJ - read inbox
@us.whitford.facade.model.agent-comms/inbox
```
