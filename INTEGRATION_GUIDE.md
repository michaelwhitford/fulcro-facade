# Integration Guide: Adding a New API to Facade

This guide walks through the complete process of integrating a new external API into Facade, using the same pattern as SWAPI and HPAPI.

## Quick Overview

Adding a new API requires 5 layers of files:

1. **OpenAPI Spec** → 2. **Component (Martian)** → 3. **Model (Logic & Resolvers)** → 4. **RAD Attributes** → 5. **UI (Forms & Reports)**

## Step-by-Step Process

### 1. Create OpenAPI Specification

**File**: `src/main/<api-name>.yml`

Create an OpenAPI 3.0 spec describing the API endpoints. Use existing specs as templates:
- `src/main/swapi.yml` - Complex API with multiple entity types and relationships
- `src/main/hpapi.yml` - Simpler API with fewer entities
- `src/main/ipapi.yml` - Single-entity API with parameters

**Key requirements**:
- `operationId` for each endpoint (Martian uses these)
- Define schemas in `components/schemas`
- Define reusable parameters in `components/parameters`
- Include response schemas for 200 and error cases

**Example**:
```yaml
openapi: 3.0.0
info:
  title: My API
  version: "0.1"

servers:
  - url: https://api.example.com
    description: "My API server"

components:
  schemas:
    Thing:
      type: object
      properties:
        id:
          type: string
        name:
          type: string

paths:
  /things/{id}:
    get:
      summary: Get a thing by ID
      operationId: thing
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      responses:
        "200":
          description: Thing details
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/Thing"
```

### 2. Create Martian HTTP Client Component

**File**: `src/main/us/whitford/facade/components/<api-name>.clj`

This component loads the OpenAPI spec and creates a Martian HTTP client.

**Template**:
```clojure
(ns us.whitford.facade.components.myapi
  (:require
    [clojure.pprint :refer [pprint]]
    [martian.core :as martian]
    [martian.httpkit :as martian-http]
    [mount.core :refer [defstate]]
    [us.whitford.facade.components.config :refer [config]]
    [us.whitford.facade.components.interceptors :as interceptors]))

(defstate myapi-martian
  :start
  (let [{:keys [swagger-file server-url]} (get config :us.whitford.facade.components.myapi/config)]
    (martian-http/bootstrap-openapi
      swagger-file
      {:server-url server-url
       :interceptors (vec (concat
                            [interceptors/tap-response]
                            martian-http/default-interceptors
                            [interceptors/tap-request]))})))

(comment
  (pprint config)
  (tap> myapi-martian)
  (martian/explore myapi-martian)
  @(martian/response-for myapi-martian :thing {:id "1"}))
```

**Key points**:
- Use `defstate` from Mount for lifecycle management
- Read config from `(get config :us.whitford.facade.components.myapi/config)`
- Include interceptors for debugging (tap-request/tap-response)
- Add comment block with test queries

### 3. Add Configuration

**File**: `src/main/config/defaults.edn`

Add API configuration to the bottom of the file:

```clojure
:us.whitford.facade.components.myapi/config {:swagger-file "myapi.yml"
                                             :server-url "https://api.example.com"}
```

**Key points**:
- Namespace the key with your component namespace
- `swagger-file` is relative to `src/main/` directory
- `server-url` should match the OpenAPI spec server

### 4. Create Model Functions and Resolvers

**File**: `src/main/us/whitford/facade/model/<api-name>.cljc`

This file contains business logic and Pathom resolvers.

**Template**:
```clojure
(ns us.whitford.facade.model.myapi
  "Functions, resolvers, and mutations supporting My API.

   DO NOT require a RAD model file in this ns. This ns is meant to be an ultimate
   leaf of the requires. Only include library code."
  (:require
   #?@(:clj [[us.whitford.facade.components.myapi :refer [myapi-martian]]
             [us.whitford.facade.components.config :refer [config]]])
   [clojure.pprint :refer [pprint]]
   [com.wsscode.pathom3.connect.operation :as pco]
   [martian.core :as martian]
   [taoensso.timbre :as log]
   [us.whitford.facade.components.utils :refer [map->nsmap]]))

#?(:clj
   (defn myapi-data
     "Fetch data from My API. Returns nil on error."
     [id opts]
     (try
       (let [req-opts (assoc opts :id id)
             {:keys [status body]} @(martian/response-for myapi-martian :thing req-opts)]
         (if (= 200 status)
           (-> body
               (map->nsmap "thing")  ; Convert keys to namespaced :thing/* keywords
               (assoc :thing/id id))
           (do
             (log/error "My API HTTP error" {:id id :status status :body body})
             nil)))
       (catch Exception e
         (log/error e "Failed to fetch thing" {:id id :opts opts})
         nil))))

#?(:clj
   (pco/defresolver thing-resolver [env {:thing/keys [id] :as params}]
     {::pco/output [:thing/id :thing/name]}
     (try
       (or (myapi-data id {}) {})
       (catch Exception e
         (log/error e "Failed to resolve thing" {:id id})
         {}))))

#?(:clj
   (pco/defresolver all-things-resolver [{:keys [query-params] :as env} params]
     {::pco/output [{:myapi/all-things [:total {:results [:thing/id :thing/name]}]}]}
     (try
       ;; Implement pagination/search if needed
       {:myapi/all-things {:results []
                           :total 0}}
       (catch Exception e
         (log/error e "Failed to resolve all-things")
         {:myapi/all-things {:results []
                             :total 0}}))))

#?(:clj (def resolvers [thing-resolver all-things-resolver]))

(comment
  (martian/explore myapi-martian)
  (myapi-data "1" {}))
```

**Key patterns**:
- Use `map->nsmap` to namespace keys (`:id` → `:thing/id`)
- Wrap all resolvers in try-catch for error handling
- Log errors with context
- Return `{}` or `nil` on error, never throw
- Export resolvers in a `resolvers` var
- Use `#?(:clj ...)` to make CLJ-only (resolvers run server-side)

### 5. Create RAD Attribute Definitions

**File**: `src/main/us/whitford/facade/model_rad/<api-name>.cljc`

Define RAD attributes for all entity fields.

**Template**:
```clojure
(ns us.whitford.facade.model-rad.myapi
  "RAD definition for My API. Attributes only."
  (:require
    [clojure.spec.alpha :as spec]
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.form-options :as fo]))

;; Thing attributes

(defattr thing_id :thing/id :string
  {ao/identity? true
   ao/required? true
   ao/schema :production})

(defattr thing_name :thing/name :string
  {ao/identities #{:thing/id}
   ao/schema :production})

;; Collection attributes

(defattr all-things :myapi/all-things :ref
  {ao/target :thing/id
   ao/pc-output [{:myapi/all-things [:total {:results '...}]}]
   ao/pc-resolve :myapi/all-things})

(def thing-attributes
  [thing_id thing_name])

(def attributes (concat thing-attributes [all-things]))
```

**Key points**:
- One attribute has `ao/identity? true` - this is the primary key
- Other attributes list their identity in `ao/identities #{}` 
- Use `:production` schema for all attributes
- Collection attributes use `:ref` type with `ao/pc-output` and `ao/pc-resolve`
- Export all attributes in an `attributes` var

### 6. Register Attributes in Main Registry

**File**: `src/main/us/whitford/facade/model_rad/attributes.cljc`

Add your attributes to the global registry:

```clojure
(ns us.whitford.facade.model-rad.attributes
  (:require
    ;; ... existing requires ...
    [us.whitford.facade.model-rad.myapi :as m.myapi]))

(def all-attributes (into []
                      (concat
                        account/attributes
                        ;; ... existing attributes ...
                        m.myapi/attributes)))  ; ADD THIS LINE
```

### 7. Register Resolvers in Parser

**File**: `src/main/us/whitford/facade/components/parser.clj`

Add your resolvers to the parser:

```clojure
(ns us.whitford.facade.components.parser
  (:require
   ;; ... existing requires ...
   [us.whitford.facade.model.myapi :as m.myapi]))  ; ADD THIS LINE

(def all-resolvers
  "The list of all hand-written resolvers/mutations."
  [m.account/resolvers
   ;; ... existing resolvers ...
   m.myapi/resolvers])  ; ADD THIS LINE
```

### 8. Create UI Components

**File**: `src/main/us/whitford/facade/ui/<api-name>_forms.cljc`

Create forms and reports for your API data.

**Form Template**:
```clojure
(ns us.whitford.facade.ui.myapi-forms
  (:require
   #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom]
      :cljs [com.fulcrologic.fulcro.dom :as dom])
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.rad.form :as form]
   [com.fulcrologic.rad.form-options :as fo]
   [com.fulcrologic.rad.report :as report]
   [com.fulcrologic.rad.report-options :as ro]
   [com.fulcrologic.statecharts.integration.fulcro.rad-integration :as ri]
   [us.whitford.facade.model-rad.myapi :as rm.myapi]))

(form/defsc-form ThingForm [this {:thing/keys [id name] :as props}]
  {fo/id             rm.myapi/thing_id
   fo/title          "Thing Details"
   fo/route-prefix   "thing"
   fo/attributes     [rm.myapi/thing_id rm.myapi/thing_name]
   fo/cancel-route   ::ThingList
   fo/read-only?     true})

(def ui-thing-form (comp/factory ThingForm))

(report/defsc-report ThingList [this props]
  {ro/title "All Things"
   ro/route "things"
   ro/source-attribute    :myapi/all-things
   ro/row-pk              rm.myapi/thing_id
   ro/columns             [rm.myapi/thing_name]
   ro/column-formatters   {:thing/name (fn [this v {:thing/keys [id] :as p}]
                                          (dom/a {:onClick #(ri/edit! this ThingForm id)} 
                                                 (str v)))}
   ro/form-links          {rm.myapi/thing_id ThingForm}})

(def ui-thing-list (comp/factory ThingList))
```

**Key points**:
- Forms use `form/defsc-form` with `fo/*` options
- Reports use `report/defsc-report` with `ro/*` options
- Use `ri/edit!` to navigate to forms
- Always create factory functions with `comp/factory`

### 9. Add Menu Items to Root UI

**File**: `src/main/us/whitford/facade/ui/root.cljc`

**Step 1: Require your components**:
```clojure
(ns us.whitford.facade.ui.root
  (:require
    ;; ... existing requires ...
    [us.whitford.facade.ui.myapi-forms :refer [ThingList ThingForm]]))
```

**Step 2: Add menu items**:
```clojure
(ui-dropdown {:className "item" :text "My API"}
  (ui-dropdown-menu {}
    (ui-dropdown-item {:onClick (fn [] (uir/route-to! this `ThingList {}))}
      (dom/i :.compact.ui.left.floated.list.icon " Things"))))
```

### 10. Register Routes in Statechart Configuration

**File**: `src/main/us/whitford/facade/client.cljs`

This is the **critical step** that enables routing. Without this, menu clicks won't load your components.

**Step 1: Require your components**:
```clojure
(ns us.whitford.facade.client
  (:require
    ;; ... existing requires ...
    [us.whitford.facade.ui.myapi-forms :refer [ThingList ThingForm]]))
```

**Step 2: Add route states to `application-chart`**:

Find the `application-chart` definition and add your components inside the `(state {:id :state/running} ...)` block:

```clojure
(def application-chart
  (statechart {:name "fulcro-swapi"}
    (uir/routing-regions
      (uir/routes {:id :region/routes
                   :routing/root Root}
        (state {:id :state/running}
          ;; ... existing routes ...
          
          ;; ADD YOUR ROUTES HERE:
          ;; For RAD reports, use ri/report-state
          (ri/report-state {:route/target `ThingList
                            :route/path   ["things"]})
          ;; For RAD forms, use ri/form-state
          (ri/form-state {:route/target `ThingForm
                          :route/path   ["thing"]})
          ;; For simple components (non-RAD), use uir/rstate
          (uir/rstate {:route/target `ThingWidget
                       :route/path   ["thing-widget"]}))))))
```

**Route state types**:

| Component Type | Route Function | When to Use |
|----------------|----------------|-------------|
| `ri/report-state` | RAD Report | Components using `report/defsc-report` |
| `ri/form-state` | RAD Form | Components using `form/defsc-form` |
| `uir/rstate` | Simple Component | Regular `defsc` components |

**Key points**:
- The `:route/path` should match the `ro/route` or `fo/route-prefix` in your UI component
- Use backtick (`) before component names to get the fully-qualified symbol
- Routes must be inside the `(state {:id :state/running} ...)` block

## Testing Your Integration

### 1. Test Martian Client
```clojure
(require '[us.whitford.facade.components.myapi :refer [myapi-martian]])
(require '[martian.core :as martian])

(martian/explore myapi-martian)
@(martian/response-for myapi-martian :thing {:id "1"})
```

### 2. Test Model Functions
```clojure
(require '[us.whitford.facade.model.myapi :as m.myapi])

(m.myapi/myapi-data "1" {})
```

### 3. Test Resolvers
```clojure
(require '[us.whitford.facade.components.parser :refer [parser]])

(parser {} [{[:thing/id "1"] [:thing/name]}])
```

### 4. Test in Browser
1. Restart server: `(require 'development) (development/restart)`
2. Reload browser
3. Navigate to your new menu item
4. Verify data loads correctly

## File Location Reference

| File Type | Location | Example |
|-----------|----------|---------|
| OpenAPI Spec | `src/main/<api>.yml` | `src/main/swapi.yml` |
| Component | `src/main/us/whitford/facade/components/<api>.clj` | `components/swapi.clj` |
| Model | `src/main/us/whitford/facade/model/<api>.cljc` | `model/swapi.cljc` |
| RAD Attributes | `src/main/us/whitford/facade/model_rad/<api>.cljc` | `model_rad/swapi.cljc` |
| UI Forms | `src/main/us/whitford/facade/ui/<api>_forms.cljc` | `ui/swapi_forms.cljc` |
| UI Root/Menu | `src/main/us/whitford/facade/ui/root.cljc` | (menu items) |
| Statechart Routes | `src/main/us/whitford/facade/client.cljs` | (application-chart) |
| Config | `src/main/config/defaults.edn` | (bottom of file) |

## Common Patterns

### Data Transformation

Use `map->nsmap` to convert API responses to namespaced keywords:

```clojure
(require '[us.whitford.facade.components.utils :refer [map->nsmap]])

;; Input: {:id "1" :name "Thing"}
;; Output: #:thing{:id "1" :name "Thing"}
(map->nsmap {:id "1" :name "Thing"} "thing")
```

### Error Handling

Always wrap resolvers in try-catch:

```clojure
#?(:clj
   (pco/defresolver my-resolver [env params]
     {::pco/output [:thing/id :thing/name]}
     (try
       (or (fetch-data params) {})
       (catch Exception e
         (log/error e "Failed to resolve" params)
         {}))))
```

### Pagination Support

For paginated APIs, use the server-paginated-report machine:

```clojure
(require '[com.fulcrologic.rad.state-machines.server-paginated-report :as spr])

(report/defsc-report MyReport [this props]
  {ro/machine spr/machine
   ro/page-size 10
   ;; ...
   })
```

### Relationships Between Entities

Define refs in RAD attributes:

```clojure
(defattr thing_owner :thing/owner :ref
  {ao/identities #{:thing/id}
   ao/target :person/id
   ao/cardinality :one})

(defattr thing_tags :thing/tags :ref
  {ao/identities #{:thing/id}
   ao/target :tag/id
   ao/cardinality :many})
```

## Checklist

Use this checklist when adding a new API:

- [ ] Create OpenAPI spec in `src/main/<api>.yml`
- [ ] Create Martian component in `components/<api>.clj`
- [ ] Add config to `config/defaults.edn`
- [ ] Test Martian client in REPL
- [ ] Create model functions in `model/<api>.cljc`
- [ ] Create Pathom resolvers in same file
- [ ] Test model functions in REPL
- [ ] Create RAD attributes in `model_rad/<api>.cljc`
- [ ] Register attributes in `model_rad/attributes.cljc`
- [ ] Register resolvers in `components/parser.clj`
- [ ] Test resolvers via parser in REPL
- [ ] Create UI forms in `ui/<api>_forms.cljc`
- [ ] Import and add menu items in `ui/root.cljc`
- [ ] **Register routes in `client.cljs` statechart** ⚠️ Critical!
- [ ] Restart server
- [ ] Test in browser
- [ ] Run linter: `clj-kondo --lint .`
- [ ] Document in `ARCHITECTURE.md` or separate file

## Examples

For complete working examples, see:

- **SWAPI** (`swapi.yml`, `components/swapi.clj`, `model/swapi.cljc`, etc.)
  - Complex: Multiple entity types, relationships, pagination
- **HPAPI** (`hpapi.yml`, `components/hpapi.clj`, `model/hpapi.cljc`, etc.)
  - Medium: Multiple entity types, filtering
- **IP API** (`ipapi.yml`, `components/ipapi.clj`, `model/ipapi.cljc`, etc.)
  - Simple: Single entity type, query parameters

## Troubleshooting

### "Namespace not found" errors
- Ensure you required the namespace in `parser.clj` and `attributes.cljc`
- Restart the server: `(development/restart)`

### "No resolver found" errors
- Check that resolvers are exported in `(def resolvers [...])`
- Verify resolvers are added to `all-resolvers` in `parser.clj`
- Check `::pco/output` matches your query keys

### Menu click does nothing / Component doesn't load
- **Most common cause**: Routes not registered in `client.cljs` statechart
- Verify component is imported in `client.cljs`
- Verify route is added to `application-chart` with correct route type:
  - `ri/report-state` for RAD reports
  - `ri/form-state` for RAD forms  
  - `uir/rstate` for simple components
- Check that `:route/path` matches `ro/route` or `fo/route-prefix`
- Reload browser after changes (hot reload may not pick up statechart changes)

### Data not loading in UI
- Check browser console for errors
- Verify resolver output matches form's query
- Use `tap>` in resolver to see if it's being called
- Test resolver directly in REPL with `parser`

### Config not loading
- Ensure config key is namespaced: `:us.whitford.facade.components.myapi/config`
- Check `swagger-file` path is relative to `src/main/`
- Restart server after config changes

## Advanced Topics

### Custom Interceptors

Add custom interceptors for auth, logging, etc.:

```clojure
(def auth-interceptor
  {:name ::auth
   :enter (fn [ctx]
            (update-in ctx [:request :headers] 
                       assoc "Authorization" "Bearer TOKEN"))})

(defstate myapi-martian
  :start
  (martian-http/bootstrap-openapi
    swagger-file
    {:server-url server-url
     :interceptors (vec (concat
                          [interceptors/tap-response
                           auth-interceptor]  ; ADD HERE
                          martian-http/default-interceptors
                          [interceptors/tap-request]))}))
```

### Mutations

Add mutations for create/update/delete operations:

```clojure
#?(:clj
   (pco/defmutation create-thing
     [env {:thing/keys [name] :as params}]
     {::pco/output [:thing/id]}
     (try
       (let [{:keys [status body]} @(martian/response-for myapi-martian :create-thing params)]
         (if (= 201 status)
           {:thing/id (:id body)}
           (do
             (log/error "Failed to create thing" {:status status :body body})
             {})))
       (catch Exception e
         (log/error e "Error creating thing" params)
         {}))))
```

### Caching

Add caching to reduce API calls:

```clojure
(def cache (atom {}))

(defn cached-fetch [id]
  (if-let [cached (get @cache id)]
    (do
      (log/debug "Cache hit for" id)
      cached)
    (let [result (myapi-data id {})]
      (swap! cache assoc id result)
      result)))
```

## See Also

- **AGENTS.md** - Development workflows, REPL operations
- **ARCHITECTURE.md** - System overview, component tables
- **MARTIAN.md** - HTTP client exploration patterns
- **RADAR.md** - Runtime introspection and debugging
