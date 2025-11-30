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
- `:counts` - resolver/mutation counts for quick overview

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

**Note:** Report columns show queryable fields, but the resolver output shape varies by API.
See "API Output Shapes Vary" below for examples.

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

### API Output Shapes Vary

Different APIs return different structures. Check actual output before assuming shape:

```clj
;; SWAPI - paginated wrapper with :total and :results
(p {} [{:swapi/all-people [:total {:results [:person/name]}]}])
;; => {:swapi/all-people {:total 82, :results [{:person/name "Luke"}...]}}

;; HPAPI - flat array (no wrapper)
(p {} [{:hpapi/all-characters [:character/name :character/house]}])
;; => {:hpapi/all-characters [{:character/name "Harry Potter"...}...]}

;; IPAPI - single entity collections
(p {} [{:ipapi/all-ip-lookups [:ip-info/query :ip-info/country]}])
```

**Tip:** If a query returns empty maps `[{} {} {}...]`, you're using the wrong shape.
Try querying without nested `:results` wrapper first.

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

## RAD Report Data Flow

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

## RAD Report Patterns

### Control Parameter Storage

**Global controls** (`:local? false` or unspecified):
```clj
;; Storage path
[::control/id <control-key> ::control/value]

;; Example
[:com.fulcrologic.rad.control/id ::search-term :com.fulcrologic.rad.control/value]
```

**Local controls** (`:local? true`):
```clj
;; Storage path
(conj report-ident :ui/parameters <control-key>)
```

### Passing Parameters to Resolvers

Use `ro/load-options` to transform control params to resolver params:
```clj
ro/load-options (fn [env]
                  (let [params (report/current-control-parameters env)
                        search-term (::search-term params)]
                    {:params (assoc params :search-term search-term)}))
```

### Triggering Report from External Component

```clj
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

## RAD Component Patterns

### Component Ident

Use keyword shorthand for simple idents:
```clj
;; ✅ CORRECT
:ident :entity/id

;; ❌ WRONG - closure doesn't have access to props during normalization
:ident (fn [] [:entity/id (:entity/id props)])
```
