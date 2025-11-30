# Fulcro RAD Concepts

> **Purpose**: Understanding RAD's attribute-driven approach - the "why" behind forms and reports.
> **Scope**: RAD layer only. See FULCRO.md for core concepts.

---

## Mental Model

RAD is **attribute-driven development**. Define your data schema once as attributes, then generate forms and reports from them.

```
┌─────────────────────────────────────────────────────────────┐
│                      Attributes                              │
│  "person/name is a required string on the person entity"    │
└─────────────────────────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
      ┌─────────┐    ┌──────────┐    ┌──────────┐
      │  Forms  │    │ Reports  │    │ Resolvers│
      │ (CRUD)  │    │ (Lists)  │    │ (Data)   │
      └─────────┘    └──────────┘    └──────────┘
```

**Key insight**: Attributes are the single source of truth. Forms, reports, and resolvers derive behavior from them.

---

## Attributes

Every piece of data is defined as an attribute:

```clojure
(defattr person_name :person/name :string
  {ao/identities #{:person/id}     ; Which entity this belongs to
   ao/required?  true})            ; Validation rule
```

### Identity Attributes

Every entity needs exactly one identity (its primary key):

```clojure
(defattr person_id :person/id :string
  {ao/identity? true})             ; Makes this the primary key
```

### Reference Attributes

Link entities together (foreign keys):

```clojure
(defattr person_homeworld :person/homeworld :ref
  {ao/identities  #{:person/id}
   ao/target      :planet/id       ; What it points to
   ao/cardinality :one})           ; :one or :many
```

### Collection Attributes

Entry points for reports:

```clojure
(defattr all-people :swapi/all-people :ref
  {ao/target     :person/id
   ao/pc-resolve :swapi/all-people})  ; Resolver that provides data
```

---

## Forms

Forms are **state machines for editing entities**. They handle dirty tracking, validation, save/cancel automatically.

```clojure
(form/defsc-form PersonForm [this props]
  {fo/id           person_id           ; Identity attribute
   fo/attributes   [person_name        ; Fields to show
                    person_height]
   fo/read-only?   true                ; View-only mode
   fo/route-prefix "person"})          ; URL: /person/:id
```

### Form Lifecycle

```
:initial → :editing → :saving → :saved
              ↑           │
              └───────────┘ (on error)
```

RAD tracks dirty fields, validates on save, and handles optimistic updates.

### Pickers (Reference Fields)

For selecting related entities:

```clojure
fo/field-styles  {:person/homeworld :pick-one}

fo/field-options {:person/homeworld
                  {po/query-key     :swapi/all-planets
                   po/query         [:planet/id :planet/name]
                   po/options-xform (fn [_ planets]
                                      (mapv #(hash-map 
                                               :text  (:planet/name %)
                                               :value [:planet/id (:planet/id %)])
                                            planets))}}
```

**Critical**: `options-xform` must return `[{:text "..." :value [:id-key id]}]` format.

---

## Reports

Reports are **state machines for data tables**. They handle loading, filtering, sorting, pagination.

```clojure
(report/defsc-report PersonList [this props]
  {ro/title            "All People"
   ro/source-attribute all-people       ; Collection attribute
   ro/row-pk           :person/id       ; Unique row identifier  
   ro/columns          [:person/name    ; Columns to display
                        :person/height]
   ro/route            "people"})       ; URL: /people
```

### Report Lifecycle

```
:initial → :loading → :loaded → :ready
                         │
                         ▼
              (filter/sort/paginate)
```

### Passing Parameters to Resolvers

Reports can have controls (search boxes, filters):

```clojure
ro/controls     {::search-term {:type :string :label "Search"}}

ro/load-options (fn [env]
                  (let [params (report/current-control-parameters env)]
                    {:params {:search-term (::search-term params)}}))
```

The resolver receives these in `:query-params`:

```clojure
(pco/defresolver all-people [{:keys [query-params]} _]
  (let [term (:search-term query-params)]
    ...))
```

---

## How RAD Uses Pathom

RAD forms and reports use Pathom resolvers for data:

| RAD Component | Pathom Usage |
|---------------|--------------|
| Form load | Entity resolver by ident `[:person/id "1"]` |
| Form save | Mutation |
| Report load | Root resolver via `ro/source-attribute` |

The `ao/pc-resolve` on collection attributes tells RAD which resolver provides the data.

---

## Common Pitfalls

### 1. Picker Options Format

**Wrong** - returning raw data:
```clojure
po/options-xform (fn [_ opts] opts)  ; ❌
```

**Right** - transform to required format:
```clojure
po/options-xform (fn [_ opts]
                   (mapv #(hash-map :text (:name %) 
                                    :value [:thing/id (:id %)])
                         opts))  ; ✅
```

### 2. Report Empty Rows

If report shows `[{} {} {}]`, the resolver output shape doesn't match what RAD expects. Check:
- `ro/row-pk` matches the ID field in results
- `ro/columns` are fields the resolver actually provides

### 3. Ident as Function

Same as core Fulcro - use keyword shorthand:
```clojure
:ident :person/id  ; ✅ Not (fn [] ...)
```

### 4. Attributes Not Registered

Attributes must be in `model_rad/attributes.cljc`:
```clojure
(def all-attributes (concat api1/attributes api2/attributes ...))
```

---

## Discover Forms & Reports

```clj
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))

;; All forms with their attributes
(->> (p {} [:radar/overview]) :radar/overview :radar/forms
     (map #(select-keys % [:name :route :id-key :attributes])))

;; All reports with their sources
(->> (p {} [:radar/overview]) :radar/overview :radar/reports
     (map #(select-keys % [:name :route :source :columns])))
```

---

## Further Reading

- [Fulcro RAD Book](https://book.fulcrologic.com/#_fulcro_rad)
- [RAD Demo](https://github.com/fulcrologic/fulcro-rad-demo)
