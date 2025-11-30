# RADAR - fulcro-radar Library

Runtime introspection for Fulcro RAD applications.

## What It Provides

Radar exposes your application's structure via EQL queries—resolvers, entities, forms, reports, relationships—all discoverable at runtime.

## Setup

```clj
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))
```

## Two Main Queries

### `:radar/overview` — System Structure

High-level view of your application:

```clj
(p {} [:radar/overview])
```

**Returns:**

| Key | Contents |
|-----|----------|
| `:radar/summary` | Counts: `{:resolvers N :entities N :reports N ...}` |
| `:radar/mount` | Mount states: `{:states ["component-a" "component-b"]}` |
| `:radar/entities` | Entity info: `[{:name "person" :fields 5 :attributes [...]}]` |
| `:radar/reports` | Report info: `[{:route "/people" :source :api/all-people :columns [...]}]` |
| `:radar/forms` | Form info: `[{:route "/person" :id-key :person/id :attributes [...]}]` |
| `:radar/references` | Entity relationships graph |

### `:radar/pathom-env` — Resolver Details

Deep introspection of Pathom resolvers:

```clj
(p {} [:radar/pathom-env])
```

**Returns:**

| Key | Contents |
|-----|----------|
| `:resolvers` | Map with `:root`, `:entity`, `:derived` resolver lists |
| `:mutations` | Available mutations: `[{:name "do-thing" :params [...]}]` |
| `:indexes` | Raw Pathom indexes (advanced) |

**Resolver shape:**
```clj
{:name "person-resolver"
 :input [:person/id]
 :output [:person/name :person/height :person/homeworld]}
```

## Resolver Categories

Radar categorizes resolvers by their input requirements:

| Category | Input | Purpose |
|----------|-------|---------|
| `:root` | None | Entry points (collections, globals) |
| `:entity` | Identity key | Fetch by ID |
| `:derived` | Non-identity | Computed/transformed data |

## Common Patterns

```clj
;; Quick status
(-> (p {} [:radar/overview]) :radar/overview :radar/summary)

;; All root resolvers (entry points)
(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :root (map :output))

;; Entity resolver fields
(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :entity
     (filter #(re-find #"person" (:name %))) first :output)

;; Report sources and columns
(->> (p {} [:radar/overview]) :radar/overview :radar/reports
     (map (juxt :source :columns)))
```

## See Also

- **AGENTS.md** — Project-specific discovery commands using radar
