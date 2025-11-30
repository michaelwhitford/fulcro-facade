# RADAR.md

fulcro-radar provides runtime introspection for Fulcro RAD applications.

## Setup

Setup parser once per session (auto-discovers namespace via mount):

```clj
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))
```

## Quick Status

```clj
(-> (p {} [:radar/overview]) :radar/overview :radar/summary)
```

## Diagnostics

Radar keys from `(p {} [:radar/overview])`:

- `:radar/summary` - mount states, attr count, entity/form/report counts
- `:radar/forms` - name, route, id-key, attributes, query
- `:radar/reports` - name, route, source resolver, row-pk, columns
- `:radar/entities` - name, fields, id-key, id-type, attributes (queryable field names)
- `:radar/references` - from/to/cardinality relationships
- `:radar/issues` - detected problems (empty = good)

Radar keys from `(p {} [:radar/pathom-env])`:

- `:resolvers` - `:root` (EQL entry points), `:entity` (by-id), `:derived`
- `:mutations` - available mutations with params/output
- `:capabilities` - what's available (parser, datomic, blob-stores)

## EQL Query Discovery

**Start with reports** - they contain working query patterns:

```clj
;; Get source resolver and columns from existing reports
(->> (p {} [:radar/overview]) :radar/overview :radar/reports
     (map (juxt :source :columns)))
;; => Pick a :source and :columns pair, then query:

;; Use report pattern directly - substitute actual :source and :columns from above
(p {} [{<:source-key> [<:column1> <:column2> ...]}])
```

**Resolver discovery** (when reports don't cover your use case):

```clj
;; Root resolvers (EQL entry points)
(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :root (map (juxt :name :output)))

;; Entity resolvers (by-id lookups) - shows input/output keys
(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :entity (map (juxt :name :input :output)))
```

## EQL Query Patterns

```clj
;; Join query - vector-wrapped, use :source/:columns from reports
(p {} [{<:source-key> [<:column1> <:column2>]}])

;; Entity lookup by ID - use :input/:output from entity resolvers
(p {} [{[<:id-key> "<actual-id>"] [<:field1> <:field2>]}])

;; Simple key query (no subselection)
(p {} [<:some-root-key>])
```

## Client State Inspection

```clj
;; Get app namespace from radar (in CLJ repl first)
(-> (p {} [:radar/overview]) :radar/overview :radar/app-ns)
;; => <app-ns>  (SPA is at <app-ns>.application/SPA)
```

```cljs
;; Then in CLJS repl, require and inspect (use app-ns from above)
;; Note: SPA is an atom wrapping the app, so double-deref is needed
(require '[com.fulcrologic.fulcro.application :as fulcro-app])
(require '[<app-ns>.application :as my-app])
(keys @(::fulcro-app/state-atom @my-app/SPA))
```
