# AGENTS.md

fulcro rad + fulcro radar app
Use PLAN.md for planning
Use CHANGELOG.md for changes
Update PLAN.md as you go to save progress

## REPL Setup

Setup parser once per session (auto-discovers namespace via mount):

```clojure
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))
```

Inspect client state from CLJS repl:

```clojure
;; Get app namespace from radar (in CLJ repl first)
(-> (p {} [:radar/overview]) :radar/overview :radar/app-ns)
;; => us.whitford.facade  (SPA is at <app-ns>.application/SPA)

;; Then in CLJS repl, require and inspect
(require '[com.fulcrologic.fulcro.application :as fulcro-app])
(require '[<app-ns>.application :as app])  ;; e.g. us.whitford.facade.application
(keys @(::fulcro-app/state-atom @app/SPA))
```

## Diagnostics (via fulcro-radar)

Quick status: `(-> (p {} [:radar/overview]) :radar/overview :radar/summary)`

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

## EQL Queries

Root resolver output determines query key. Find available resolvers:

```clojure
;; Root resolvers (EQL entry points)
(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :root (map (juxt :name :output)))

;; Entity resolvers (by-id lookups) - shows required input keys
(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :entity (map (juxt :name :input)))
```

Join queries must be vector-wrapped (substitute actual resolver/attribute names):

```clojure
;; Correct - vector-wrapped join (use actual resolver output key from :root)
(p {} [{:example/all-items [:item/id :item/name]}])

;; Simple key query (no subselection)
(p {} [:example/all])

;; Entity lookup by ID (ident pattern - use actual entity id-key and UUID)
(p {} [{[:item/id "actual-uuid-here"]
        [:item/name :item/field1 :item/field2]}])
```

## RAD Gotchas

- **Ident**: Use `:ident :id-key` shorthand, not closures
- **Report stuck**: Check `:ui/cache :filtered-rows`, trigger `(uism/trigger! @SPA ident :event/loaded)`

## Restart

```cljs
(require 'development)(development/restart)
```

## Operations

```bash
clojure -M:run-tests # kaocha tests
clj-kondo --lint . # clj-kondo lint
clojure -M:outdated # check dependencies
```

## Tests

`.cljc` files with fulcro-spec. `let` bindings outside `assertions` block, use `=>` operator.
