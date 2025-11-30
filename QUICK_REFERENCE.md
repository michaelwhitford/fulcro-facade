# Quick Reference Card

Essential patterns and commands for working with Facade.

## Development Workflow

```clojure
;; Restart server (after CLJ changes)
(require 'development)
(development/restart)

;; CLJS hot-reloads automatically via shadow-cljs
```

## File Locations

| Type | Path | Example |
|------|------|---------|
| OpenAPI Specs | `src/main/*.yml` | `swapi.yml` |
| Components (CLJ) | `src/main/us/whitford/facade/components/*.clj` | `swapi.clj` |
| Models (CLJC) | `src/main/us/whitford/facade/model/*.cljc` | `swapi.cljc` |
| RAD Attributes | `src/main/us/whitford/facade/model_rad/*.cljc` | `swapi.cljc` |
| UI (CLJC) | `src/main/us/whitford/facade/ui/*.cljc` | `swapi_forms.cljc` |
| Config | `src/main/config/*.edn` | `defaults.edn` |

## Component Types - When to Use What

| Type | Use When | Provides | See |
|------|----------|----------|-----|
| `defsc` | Custom UI, no CRUD | Manual state management | FULCRO.md |
| `defsc-form` | Editing single entity | Save/cancel, validation, dirty tracking | FULCRO-RAD.md |
| `defsc-report` | Showing list of entities | Filter, sort, pagination | FULCRO-RAD.md |

**Decision flowchart**:
- Need a list of data? → `defsc-report`
- Need to edit an entity? → `defsc-form`  
- Need custom behavior? → `defsc`

## Common Pitfalls

### 1. Ident as Function
❌ **DON'T**:
```clojure
:ident (fn [] [:entity/id (:entity/id props)])
```

✅ **DO**:
```clojure
:ident :entity/id
```

**Why**: Normalization happens before props are available. See FULCRO.md for details.

### 2. Picker Options Format
❌ **DON'T**:
```clojure
po/options-xform (fn [_ opts] opts)  ; Return raw data
```

✅ **DO**:
```clojure
po/options-xform (fn [_ opts]
                   (mapv #(hash-map :text (:name %)
                                    :value [:thing/id (:id %)])
                         opts))
```

**Why**: Pickers need `{:text "Display" :value [:id key]}` format. See FULCRO-RAD.md for details.

### 3. Always Return Map from Resolvers
❌ **DON'T**:
```clojure
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/output [:person/name]}
  (fetch-person id))  ; Might return nil!
```

✅ **DO**:
```clojure
(pco/defresolver person-resolver [{:person/keys [id]}]
  {::pco/output [:person/name]}
  (try
    (or (fetch-person id) {})
    (catch Exception e
      (log/error e "Failed" {:id id})
      {})))
```

**Why**: Pathom expects maps. Returning nil breaks the query. See PATHOM.md for details.

## Common Tasks

### Explore an API with Martian

```clojure
(require '[martian.core :as martian])
(require '[us.whitford.facade.components.swapi :refer [swapi-martian]])

(martian/explore swapi-martian)              ; List operations
(martian/explore swapi-martian :person)      ; Operation details
@(martian/response-for swapi-martian :person {:id "1"})  ; Execute
```

### Test a Resolver

```clojure
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))

;; Query by entity ID
(p {} [{[:person/id "1"] [:person/name :person/height]}])

;; Query collection
(p {} [{:swapi/all-people [:total {:results [:person/name]}]}])
```

### Inspect App State

```clojure
(require '[us.whitford.facade.application :refer [SPA]])
(require '[com.fulcrologic.fulcro.application :as app])

@(::app/state-atom @SPA)  ; Full app state
```

### Runtime Introspection

```clojure
;; System overview
(p {} [:radar/overview])

;; All resolvers
(p {} [:radar/pathom-env])

;; UI component registry  
(p {} [:radar/ui-registry])
```

See `RADAR.md` for comprehensive introspection patterns.

## Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                         Browser (CLJS)                       │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ UI Components (ui/*.cljc)                              │ │
│  │   - Forms (form/defsc-form)                            │ │
│  │   - Reports (report/defsc-report)                      │ │
│  │   - Regular components (defsc)                         │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                             │ EQL Queries
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                        Server (CLJ)                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Parser (components/parser.clj)                         │ │
│  │   - Routes EQL queries to resolvers                    │ │
│  └────────────────────────────────────────────────────────┘ │
│                             │                                │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Resolvers (model/*.cljc)                               │ │
│  │   - pco/defresolver - Read data                        │ │
│  │   - pco/defmutation - Write data                       │ │
│  └────────────────────────────────────────────────────────┘ │
│                             │                                │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Martian Clients (components/*.clj)                     │ │
│  │   - HTTP clients for external APIs                     │ │
│  │   - Load OpenAPI specs                                 │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                             │ HTTP
                             ▼
                      External APIs
                    (swapi.dev, etc.)
```

## RAD Attributes

```clojure
;; Identity attribute (primary key)
(defattr thing_id :thing/id :string
  {ao/identity? true
   ao/required? true
   ao/schema :production})

;; Regular attribute
(defattr thing_name :thing/name :string
  {ao/identities #{:thing/id}    ; Points to identity
   ao/schema :production})

;; Reference (foreign key)
(defattr thing_owner :thing/owner :ref
  {ao/identities #{:thing/id}
   ao/target :person/id           ; Target entity
   ao/cardinality :one})          ; :one or :many

;; Collection (for reports)
(defattr all-things :myapi/all-things :ref
  {ao/target :thing/id
   ao/pc-output [{:myapi/all-things [:total {:results '...}]}]
   ao/pc-resolve :myapi/all-things})
```

## Pathom Resolvers

```clojure
;; Single entity resolver
#?(:clj
   (pco/defresolver thing-resolver [env {:thing/keys [id] :as params}]
     {::pco/output [:thing/id :thing/name]}
     (try
       (or (fetch-thing id) {})
       (catch Exception e
         (log/error e "Failed" {:id id})
         {}))))

;; Collection resolver (for reports)
#?(:clj
   (pco/defresolver all-things-resolver [{:keys [query-params]} params]
     {::pco/output [{:myapi/all-things [:total {:results [:thing/id :thing/name]}]}]}
     (try
       {:myapi/all-things {:results (fetch-all-things)
                           :total (count-things)}}
       (catch Exception e
         {:myapi/all-things {:results [] :total 0}}))))

;; Mutation
#?(:clj
   (pco/defmutation create-thing [env {:thing/keys [name] :as params}]
     {::pco/output [:thing/id]}
     (try
       (let [id (save-thing! params)]
         {:thing/id id})
       (catch Exception e
         (log/error e "Create failed" params)
         {}))))
```

## Mount Components

```clojure
;; Define component
(defstate my-component
  :start (initialize-component)
  :stop (shutdown-component))

;; Start all components
(mount/start)

;; Stop all components  
(mount/stop)

;; Restart (via development ns)
(development/restart)
```

## Common Utilities

```clojure
;; Namespace map keys
(require '[us.whitford.facade.components.utils :refer [map->nsmap]])
(map->nsmap {:id "1" :name "Thing"} "thing")
;; => #:thing{:id "1" :name "Thing"}

;; String to int
(require '[us.whitford.facade.components.utils :refer [str->int]])
(str->int "42")
;; => 42
```

## Testing Commands

```bash
# Run tests
clojure -M:run-tests

# Lint
clj-kondo --lint .

# Check deps
clojure -M:outdated
```

## Configuration

Config files are merged hierarchically:
1. `config/defaults.edn` (base)
2. `config/dev.edn` or `config/prod.edn` (environment-specific)
3. JVM `-Dconfig` property (override)

Access in code:
```clojure
(require '[us.whitford.facade.components.config :refer [config]])

;; Get component config
(get config :us.whitford.facade.components.myapi/config)
```

## Debugging

```clojure
;; Tap values for Portal/REPL
(tap> {:debug-info "here"})

;; Log with context
(require '[taoensso.timbre :as log])
(log/info "Message" {:context "data"})
(log/error exception "Message" {:context "data"})

;; Check parser request/response (in logs)
;; Automatically logged by pathom-common middleware
```

## Key Namespaces

| Namespace | Purpose |
|-----------|---------|
| `com.fulcrologic.fulcro.components` | UI component macros |
| `com.fulcrologic.rad.form` | Form definitions |
| `com.fulcrologic.rad.report` | Report definitions |
| `com.fulcrologic.rad.attributes` | Attribute definitions |
| `com.wsscode.pathom3.connect.operation` | Resolver/mutation macros |
| `martian.core` | HTTP client operations |
| `mount.core` | Component lifecycle |

## Documentation Index

### Framework Guides (Learn Concepts)

| Document | Purpose |
|----------|---------|
| `FULCRO.md` | ⭐ Fulcro core (normalization, queries, idents) |
| `FULCRO-RAD.md` | ⭐ RAD framework (forms, reports, attributes) |
| `PATHOM.md` | ⭐ Pathom3 resolvers (input/output, query planning) |
| `STATECHARTS.md` | Statechart patterns (routing, backend workflows) |

### Project Guides (Get Things Done)

| Document | Purpose |
|----------|---------|
| `QUICK_REFERENCE.md` | This file - Common tasks and patterns |
| `INTEGRATION_GUIDE.md` | Add new API (step-by-step) |
| `AGENTS.md` | AI agent instructions, REPL commands |
| `ARCHITECTURE.md` | System overview, component tables |
| `RADAR.md` | Runtime introspection patterns |
| `EQL.md` | Query patterns and copy-paste examples |
| `MARTIAN.md` | HTTP client exploration |
| `TROUBLESHOOTING.md` | Common issues and solutions |
| `PLAN.md` | Feature documentation |
| `CHANGELOG.md` | Version history |
| `TODO.md` | Tech debt and potential improvements |

## Examples

All four API integrations are complete working examples:

- **SWAPI** - Complex: Multiple entities, relationships, pagination
- **HPAPI** - Medium: Multiple entities, filtering
- **IP API** - Simple: Single entity, query parameters
- **Wttr** - Simple: Weather API, bridge resolver (uses IP location)

Files for each: `<api>.yml`, `components/<api>.clj`, `model/<api>.cljc`, `model_rad/<api>.cljc`, `ui/<api>_forms.cljc`
