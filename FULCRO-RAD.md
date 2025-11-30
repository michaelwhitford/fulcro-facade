# Fulcro RAD Framework Guide

> **Purpose**: This document explains Fulcro RAD concepts and patterns used in this project.  
> **Audience**: AI agents and developers new to Fulcro RAD.  
> **Scope**: RAD-specific features (forms, reports, attributes). See FULCRO.md for core framework concepts.

---

## Table of Contents

1. [What is RAD?](#what-is-rad)
2. [RAD Attributes](#rad-attributes)
3. [RAD Forms](#rad-forms)
4. [RAD Reports](#rad-reports)
5. [RAD Pickers](#rad-pickers)
6. [Common Patterns](#common-patterns)
7. [Troubleshooting](#troubleshooting)

---

## What is RAD?

**RAD** (Rapid Application Development) is a layer on top of Fulcro that provides:
- **Attribute-driven development** - Define schema once, use everywhere
- **Generated UI** - Forms and reports auto-generate from attributes
- **State machines** - Built-in form and report lifecycle management
- **Conventions** - Standard patterns for CRUD operations

### When to Use RAD

✅ **Use RAD when**:
- Building CRUD applications
- You want conventions over configuration
- You need forms with validation and save logic
- You need data tables with filtering/sorting/pagination

❌ **Don't use RAD when**:
- Building highly custom UIs that don't fit form/report patterns
- You need complete control over state management
- The overhead of attributes doesn't justify the convenience

---

## RAD Attributes

Attributes are the **foundation of RAD**. They define your data schema, validation rules, UI hints, and database mapping.

### Anatomy of an Attribute

```clojure
(defattr person_name :person/name :string
  {ao/identities    #{:person/id}      ; Which entity this belongs to
   ao/required?     true                ; Validation: field required
   ao/schema        :production         ; Which schema (DB) this uses
   ao/field-labels  {:en "Full Name"}}) ; UI: how to label this field
```

### Attribute Types

| Type | Example | Use For |
|------|---------|---------|
| `:string` | Names, emails | Text data |
| `:int` | Ages, counts | Numbers |
| `:boolean` | Active flags | True/false |
| `:instant` | Timestamps | Dates/times |
| `:ref` | Foreign keys | References to other entities |
| `:enum` | Status codes | Fixed set of values |

### Identity Attributes

**Every entity needs one identity attribute**:

```clojure
(defattr person_id :person/id :string
  {ao/identity? true     ; ⭐ This makes it an identity
   ao/required? true
   ao/schema :production})  ; ⭐ Required for Datomic persistence
```

**Rules**:
- Must be unique per entity
- Used for normalization (Fulcro's idents)
- Can be `:string`, `:int`, or `:uuid`

**Note**: `ao/schema` is required for Datomic-persisted entities (like `account`). For read-only external API entities (like SWAPI), it can be omitted.

### Reference Attributes

**Link entities together**:

```clojure
(defattr person_homeworld :person/homeworld :ref
  {ao/identities  #{:person/id}
   ao/target      :planet/id           ; ⭐ What this references
   ao/cardinality :one})               ; ⭐ :one or :many
```

**Cardinality**:
- `:one` - Single reference (e.g., person has one homeworld)
- `:many` - Multiple references (e.g., person has many films)

### Collection Attributes

**For reports that load lists of entities**:

```clojure
(defattr all-people :swapi/all-people :ref
  {ao/target      :person/id
   ao/pc-output   [{:swapi/all-people [:total {:results '...}]}]  ; ⭐ Shape of data
   ao/pc-resolve  :swapi/all-people})  ; ⭐ Resolver that provides this
```

**When to use**:
- Define entry points for RAD reports
- Specify what resolver provides the data
- Document the expected data shape

---

## RAD Forms

Forms are **state machine-driven CRUD interfaces** with automatic validation, save/cancel, and dirty tracking.

### Form Lifecycle

```
┌──────────────┐
│  :initial    │ ← Form created
└──────┬───────┘
       │
       ↓ User edits
┌──────────────┐
│  :editing    │ ← Changes tracked
└──────┬───────┘
       │
       ↓ User clicks Save
┌──────────────┐
│  :saving     │ ← Mutation runs
└──────┬───────┘
       │
       ↓ Save completes
┌──────────────┐
│  :success    │ ← Navigate away or reset
└──────────────┘
```

### Basic Form Definition

```clojure
(form/defsc-form PersonForm [this {:person/keys [id name] :as props}]
  {fo/id           person_id                    ; Identity attribute
   fo/title        "Person Details"             ; Display title
   fo/route-prefix "person"                     ; URL route
   fo/attributes   [person_id person_name       ; Which attributes to show
                    person_height person_mass]
   fo/cancel-route ::PersonList}                ; Where to go on cancel
  
  ;; Render (optional - RAD can auto-generate)
  (dom/div "Custom rendering here"))
```

### Form Options Reference

| Option | Type | Purpose | Example |
|--------|------|---------|---------|
| `fo/id` | Attribute | Identity attribute | `person_id` |
| `fo/title` | String | Form header | `"Edit Person"` |
| `fo/route-prefix` | String | URL path | `"person"` |
| `fo/attributes` | Vector | Fields to show | `[person_name person_age]` |
| `fo/cancel-route` | Keyword | Navigation target | `::PersonList` |
| `fo/read-only?` | Boolean | Disable editing | `true` |
| `fo/field-styles` | Map | How to render fields | `{:person/homeworld :pick-one}` |
| `fo/field-options` | Map | Field configuration | See below |

### Field Styles

**Control how fields render**:

```clojure
fo/field-styles {:person/gender    :radio         ; Radio buttons
                 :person/homeworld :pick-one      ; Dropdown picker
                 :person/films     :pick-many}    ; Multi-select picker
```

| Style | Use For | Renders As |
|-------|---------|------------|
| `:text` | Short text | Input box |
| `:textarea` | Long text | Text area |
| `:dropdown` | Selection | Dropdown menu |
| `:radio` | Few options | Radio buttons |
| `:pick-one` | Reference | Picker dropdown |
| `:pick-many` | References | Multi-select |

### Pickers (Reference Fields)

**For selecting related entities**:

```clojure
fo/field-options {:person/homeworld 
                  {po/query-key       :swapi/all-planets  ; ⭐ Where to get options
                   po/query           [:planet/id         ; ⭐ What to load
                                       :planet/name]
                   po/cache-time-ms   30000               ; ⭐ Cache for 30sec
                   po/options-xform   (fn [_ options]     ; ⭐ Transform to dropdown format
                                        (mapv (fn [{:planet/keys [id name]}]
                                                {:text name 
                                                 :value [:planet/id id]})
                                              options))}}
```

**Picker Options**:
- `po/query-key` - Collection attribute that provides options
- `po/query` - EQL query for option data
- `po/cache-time-ms` - How long to cache options
- `po/options-xform` - Function to transform data to `{:text "..." :value [...]}` format
- `po/allow-create?` - Allow creating new entities
- `po/allow-edit?` - Allow editing selected entity

### Read-Only Forms

**For viewing data without editing**:

```clojure
(form/defsc-form PersonForm [this props]
  {fo/id          person_id
   fo/read-only?  true        ; ⭐ Disables all editing
   fo/attributes  [person_id person_name]})
```

---

## RAD Reports

Reports are **state machine-driven data tables** with automatic filtering, sorting, pagination, and export.

### Report Lifecycle

```
┌─────────────┐
│ :initial    │ ← Report created
└──────┬──────┘
       │
       ↓ Load triggered
┌─────────────┐
│ :loading    │ ← Fetch data via resolver
└──────┬──────┘
       │
       ↓ Data arrives
┌─────────────┐
│ :processing │ ← Filter/sort/paginate
└──────┬──────┘
       │
       ↓ Ready to show
┌─────────────┐
│ :ready      │ ← Render table
└─────────────┘
```

### Basic Report Definition

```clojure
(report/defsc-report PersonList [this props]
  {ro/title            "All People"               ; Display title
   ro/source-attribute all-people                 ; ⭐ Collection attribute
   ro/row-pk           :person/id                 ; ⭐ Unique row identifier
   ro/columns          [:person/name              ; ⭐ Which columns to show
                        :person/height
                        :person/mass]
   ro/route            "people"}                  ; URL route
  
  ;; Render (optional - RAD can auto-generate)
  (dom/div "Custom table rendering"))
```

### Report Options Reference

| Option | Type | Purpose | Example |
|--------|------|---------|---------|
| `ro/title` | String | Report header | `"All People"` |
| `ro/source-attribute` | Attribute | Data source | `all-people` |
| `ro/row-pk` | Keyword | Unique row ID | `:person/id` |
| `ro/columns` | Vector | Columns to show | `[:person/name]` |
| `ro/route` | String | URL path | `"people"` |
| `ro/controls` | Map | Search/filter controls | See below |
| `ro/load-options` | Function | How to pass params | See below |

### Report Controls (Search/Filter)

**Add search/filter inputs**:

```clojure
ro/controls {::search-term                       ; ⭐ Control ID
             {:type   :string                    ; Input type
              :label  "Search"                   ; Label
              :local? false}}                    ; Global vs local state
```

**Control Storage**:
- `local? false` (default) - Stored globally: `[::control/id ::search-term ::control/value]`
- `local? true` - Stored in report: `(conj report-ident :ui/parameters ::search-term)`

### Passing Parameters to Resolvers

**Use `ro/load-options` to transform control values into resolver params**:

```clojure
ro/load-options (fn [env]
                  ;; env contains: state-map, app, report instance
                  (let [params (report/current-control-parameters env)  ; ⭐ Get control values
                        search-term (::search-term params)]
                    {:params {:search-term search-term}}))              ; ⭐ Pass to resolver
```

**Flow**:
1. User types in search control → stored in app state
2. Report loads → calls `ro/load-options` with env
3. `ro/load-options` reads controls → returns `{:params {...}}`
4. Params passed to resolver → resolver receives in `query-params`

### Server-Paginated Reports

**For large datasets that can't load all at once**:

```clojure
(report/defsc-report PersonList [this props]
  {ro/title            "All People"
   ro/source-attribute all-people
   ro/row-pk           :person/id
   ro/columns          [:person/name]
   ro/paginate?        true                      ; ⭐ Enable pagination
   ro/page-size        20}                       ; ⭐ Rows per page
```

**Server requirements**:
- Resolver must accept `:page` and `:page-size` params
- Resolver must return `{:total X :results [...]}`

---

## RAD Pickers

Pickers let users **select related entities** (foreign keys) via dropdowns or search.

### Picker Configuration

```clojure
fo/field-options {:person/homeworld
                  {po/query-key      :swapi/all-planets   ; Data source
                   po/query          [:planet/id          ; What to load
                                      :planet/name]
                   po/cache-time-ms  30000                ; Cache duration
                   po/options-xform  (fn [_ options]      ; Transform to dropdown format
                                       (mapv #(hash-map :text (:planet/name %)
                                                        :value [:planet/id (:planet/id %)])
                                             options))}}
```

### Picker Option Format

**Pickers expect options in this format**:

```clojure
[{:text "Tatooine" :value [:planet/id "1"]}
 {:text "Alderaan" :value [:planet/id "2"]}]
```

**Rules**:
- `:text` - What user sees
- `:value` - Ident tuple `[<identity-keyword> <id>]`
- Transform your data to this format in `po/options-xform`

---

## Common Patterns

### Pattern: Read-Only Detail View

**Show entity details without editing**:

```clojure
(form/defsc-form PersonForm [this props]
  {fo/id              person_id
   fo/title           "Person Details"
   fo/route-prefix    "person"
   fo/attributes      [person_id person_name person_height]
   fo/read-only?      true              ; ⭐ No editing
   fo/silent-abandon? true              ; ⭐ No confirm on navigate
   fo/cancel-route    ::PersonList})
```

### Pattern: Report with Search

**Table with search box**:

```clojure
(report/defsc-report PersonList [this props]
  {ro/title            "All People"
   ro/source-attribute all-people
   ro/row-pk           :person/id
   ro/columns          [:person/name :person/height]
   
   ro/controls         {::search-term {:type :string :label "Search"}}  ; ⭐ Add search
   
   ro/load-options     (fn [env]                                        ; ⭐ Pass to resolver
                         (let [params (report/current-control-parameters env)
                               search-term (::search-term params)]
                           {:params {:search-term search-term}}))})
```

### Pattern: Form with Pickers

**Form with dropdown to select related entity**:

```clojure
(form/defsc-form PersonForm [this props]
  {fo/id             person_id
   fo/attributes     [person_id person_name person_homeworld]
   
   fo/field-styles   {:person/homeworld :pick-one}                     ; ⭐ Render as picker
   
   fo/field-options  {:person/homeworld 
                      {po/query-key :swapi/all-planets                 ; ⭐ Data source
                       po/query [:planet/id :planet/name]
                       po/options-xform (fn [_ planets]
                                          (mapv #(hash-map :text (:planet/name %)
                                                           :value [:planet/id (:planet/id %)])
                                                planets))}}})
```

### Pattern: Navigating to a Form

**Open entity detail form from a report**:

```clojure
;; In a report row click handler:
(defn row-clicked [app entity-id]
  (ri/edit! app PersonForm entity-id))  ; ⭐ Navigate to form in edit mode

;; For read-only view:
(defn row-clicked [app entity-id]
  (ri/view! app PersonForm entity-id))  ; ⭐ Navigate to form in read-only mode
```

---

## Troubleshooting

### "Attribute not found" Error

**Problem**: RAD can't find an attribute you're using.

**Cause**: Attributes must be registered in `model_rad/attributes.cljc`:

```clojure
(def all-attributes
  (vec (concat
    swapi/attributes      ; ⭐ Make sure your attributes are included
    hpapi/attributes
    entity/attributes)))
```

**Solution**: Add your attribute namespace to the `all-attributes` vector.

---

### Ident Must Be Keyword, Not Function

**Problem**: This doesn't work:
```clojure
:ident (fn [] [:entity/id (:entity/id props)])  ; ❌ WRONG
```

**Cause**: Normalization happens **before** props are available.

**Solution**: Use keyword shorthand:
```clojure
:ident :entity/id  ; ✅ CORRECT
```

---

### Picker Shows No Options

**Problem**: Dropdown is empty even though data exists.

**Common causes**:
1. **Wrong query-key**: Check that `:po/query-key` matches a collection attribute
2. **Cache expired**: Data was cached but expired - try refreshing
3. **Transform error**: `po/options-xform` must return `[{:text "..." :value [...]}]` format
4. **Query missing fields**: `po/query` must include fields used in transform

**Debug**:
```clojure
po/options-xform (fn [_ options]
                   (tap> {:picker-options options})  ; ⭐ See what data you're getting
                   (mapv transform-fn options))
```

---

### Report Shows Empty Rows

**Problem**: Report loads but all rows are empty `{}`.

**Common causes**:
1. **Resolver returns wrong shape**: Check that resolver returns`{:total X :results [...]}`
2. **Row PK mismatch**: `:ro/row-pk` doesn't match the ID field in results
3. **Columns don't match**: `:ro/columns` reference attributes not in resolver output

**Debug**:
```clojure
;; In resolver:
(defresolver all-people-resolver [env params]
  {::pco/output [{:swapi/all-people [:total {:results [:person/id :person/name]}]}]}
  (let [result {:swapi/all-people {:total 10 :results [...]}}]
    (tap> {:resolver-output result})  ; ⭐ See what you're returning
    result))
```

---

### Form Won't Save

**Problem**: Clicking Save does nothing or shows error.

**Common causes**:
1. **No save middleware**: Check that save middleware is registered in parser
2. **Validation errors**: Form has validation errors blocking save
3. **Missing identity**: Entity missing required identity attribute

**Debug**:
```clojure
fo/debug? true  ; ⭐ Enable form debugging (logs to console)
```

---

## Next Steps

- See **FULCRO.md** for core Fulcro concepts (normalization, queries, state)
- See **PATHOM.md** for resolver patterns and Pathom query engine
- See **EQL.md** for copy-paste ready query examples
- See **QUICK_REFERENCE.md** for project-specific code snippets
- See **INTEGRATION_GUIDE.md** for adding new API integrations

---

## Further Reading

- [Fulcro RAD Book](https://book.fulcrologic.com/#_fulcro_rad)
- [RAD Demo](https://github.com/fulcrologic/fulcro-rad-demo)
- [Fulcro Developers Guide](https://book.fulcrologic.com)
