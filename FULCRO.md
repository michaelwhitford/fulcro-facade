# Fulcro Framework Guide

> **Purpose**: Core Fulcro concepts - the "why" behind the patterns.
> **Scope**: Core Fulcro only. See FULCRO-RAD.md for RAD extensions.

---

## Table of Contents

1. [Mental Model](#mental-model)
2. [Normalization and Idents](#normalization-and-idents)
3. [EQL Queries](#eql-queries)
4. [Initial State](#initial-state)
5. [Data Loading](#data-loading)
6. [Mutations](#mutations)
7. [Component Rendering](#component-rendering)
8. [Common Pitfalls](#common-pitfalls)

---

## Mental Model

Fulcro is a **graph database UI framework**. Think of it as:

```
┌─────────────────────────────────────────────────────────────┐
│                    Normalized State                          │
│  (Single source of truth - like a local database)           │
│                                                              │
│  {:person/id {1 {...} 2 {...}}                              │
│   :film/id   {1 {...} 2 {...}}}                             │
└─────────────────────────────────────────────────────────────┘
                           ▲
                           │ Queries (EQL)
                           │
┌─────────────────────────────────────────────────────────────┐
│                      Components                              │
│  (Declare what data they need, receive denormalized props)  │
└─────────────────────────────────────────────────────────────┘
```

**Key insight**: Components don't fetch data. They declare what they need (query), and Fulcro denormalizes the graph to provide props.

### vs React + Redux

| Concept | Redux | Fulcro |
|---------|-------|--------|
| State shape | You design it | Normalized by default |
| Data fetching | Manual (thunks, sagas) | Declarative (load!) |
| Query language | None (selectors) | EQL |
| Server integration | Separate | Same query language |

### vs Re-frame

| Concept | Re-frame | Fulcro |
|---------|----------|--------|
| State | Flat app-db | Normalized graph |
| Subscriptions | Manual | Automatic from queries |
| Data fetching | Effects | Built-in load! |
| Normalization | Manual | Automatic |

---

## Normalization and Idents

Fulcro stores app state as a **normalized graph database**. This is the core concept.

### Why Normalize?

**Without normalization** - data duplicated, updates are error-prone:

```clojure
{:current-user {:user/id 1 :user/name "Alice"}
 :messages [{:message/author {:user/id 1 :user/name "Alice"}}  ; duplicate!
            {:message/author {:user/id 1 :user/name "Alice"}}]} ; duplicate!
```

**With normalization** - single source of truth:

```clojure
{:user/id {1 {:user/id 1 :user/name "Alice"}}     ; stored once
 :message/id {1 {:message/author [:user/id 1]}    ; reference
              2 {:message/author [:user/id 1]}}}  ; same reference
```

Update Alice's name once → all references see the change.

### Idents

An **ident** is `[<table-key> <id-value>]` - a pointer into the normalized state:

```clojure
[:person/id "1"]      ; Points to person with ID "1"
[:film/id "4"]        ; Points to film with ID "4"
```

### Component Idents

Components declare their ident to enable normalization:

```clojure
(defsc Person [this {:person/keys [name]}]
  {:query [:person/id :person/name]
   :ident :person/id}  ; ⭐ Keyword shorthand (recommended)
  (dom/div name))
```

**Critical rule**: Always use keyword shorthand for `:ident`. The function form `(fn [] ...)` runs during normalization before props exist - a common source of bugs.

---

## EQL Queries

Components declare what data they need using **EQL** (EDN Query Language).

### Query Types

```clojure
;; Properties
[:person/name :person/height]

;; Joins (nested data)
[{:person/films [:film/title]}]

;; Ident lookup (specific entity)
[{[:person/id "1"] [:person/name]}]

;; Parameterized
[({:search/results [:entity/name]} {:term "luke"})]
```

### Query Composition

Child queries compose into parent queries automatically:

```clojure
(defsc Film [this {:film/keys [title]}]
  {:query [:film/id :film/title]
   :ident :film/id}
  ...)

(defsc Person [this {:person/keys [name films]}]
  {:query [:person/id :person/name
           {:person/films (comp/get-query Film)}]  ; ⭐ Include child query
   :ident :person/id}
  ...)
```

### Link Queries (References)

When data contains idents (references), the query follows them:

```clojure
;; State has reference:
{:person/id {"1" {:person/name "Luke"
                  :person/homeworld [:planet/id "1"]}}}  ; ident reference

;; Query with join follows reference:
[:person/name {:person/homeworld [:planet/name]}]

;; Props received (denormalized):
{:person/name "Luke"
 :person/homeworld {:planet/name "Tatooine"}}
```

---

## Initial State

Components can declare initial state for pre-populating the normalized database:

```clojure
(defsc Person [this {:person/keys [name]}]
  {:query [:person/id :person/name]
   :ident :person/id
   :initial-state {:person/id :param/id        ; :param/* pulls from args
                   :person/name :param/name}}
  ...)

;; Used by parent:
(defsc Root [this {:keys [current-person]}]
  {:query [{:current-person (comp/get-query Person)}]
   :initial-state {:current-person {:id "1" :name "Default"}}}
  ...)
```

**When to use**: Pre-loading UI state, default values, component-local UI state (`:ui/*` keys).

---

## Data Loading

Fulcro provides `df/load!` to fetch data from the server:

```clojure
(require '[com.fulcrologic.fulcro.data-fetch :as df])

;; Load into a specific ident (entity by ID)
(df/load! app [:person/id "1"] Person)

;; Load into a root key
(df/load! app :all-people PersonList)

;; Load with parameters
(df/load! app :search-results SearchResult
  {:params {:term "luke"}})
```

### Load Lifecycle

```
df/load! called
    ↓
EQL query sent to server (via Pathom)
    ↓
Response normalized into state
    ↓
Components re-render with new data
```

### Load Markers

Track loading state with markers:

```clojure
(df/load! app :people PersonList
  {:marker :loading-people})

;; In component, check marker:
(when (df/loading? (get props :ui/loading-people))
  (dom/div "Loading..."))
```

---

## Mutations

Mutations are how you change state. They can be local-only or remote.

```clojure
(defmutation set-name [{:keys [id name]}]
  (action [{:keys [state]}]                  ; Local state change
    (swap! state assoc-in [:person/id id :person/name] name))

  (remote [env] true))                       ; Also send to server

;; Call from component:
(comp/transact! this [(set-name {:id "1" :name "New Name"})])
```

### Optimistic Updates

The `action` runs immediately (optimistic). If `remote` returns true, it's sent to server. On error, Fulcro can roll back.

```clojure
(defmutation save-entity [params]
  (action [{:keys [state]}]
    (swap! state assoc-in [...] {:saving? true}))

  (remote [env] true)

  (ok-action [{:keys [state]}]               ; Server succeeded
    (swap! state assoc-in [...] {:saving? false}))

  (error-action [{:keys [state]}]            ; Server failed
    (swap! state assoc-in [...] {:saving? false :error true})))
```

---

## Component Rendering

Components receive **denormalized props** based on their query:

```clojure
(defsc Person [this {:person/keys [name films]}]
  {:query [:person/id :person/name 
           {:person/films [:film/id :film/title]}]
   :ident :person/id}

  ;; `films` is a vector of denormalized film maps, not idents
  (dom/div
    (dom/h1 name)
    (dom/ul
      (map #(dom/li {:key (:film/id %)} (:film/title %)) films))))
```

The component doesn't see idents - Fulcro resolves them automatically based on the query.

---

## Common Pitfalls

### 1. Ident as Function

**Wrong** - props not available during normalization:
```clojure
:ident (fn [] [:person/id (:person/id props)])   ; ❌
```

**Right** - keyword shorthand:
```clojure
:ident :person/id                                ; ✅
```

### 2. Missing Ident

If data isn't normalizing, check that the component has `:ident`:
```clojure
(defsc Person [this props]
  {:query [:person/id :person/name]
   :ident :person/id}  ; ⭐ Required for normalization
  ...)
```

### 3. Rendering Before Data Loads

Always handle nil/loading states:
```clojure
(defsc Person [this {:person/keys [name]}]
  {:query [:person/name]}
  (if name
    (dom/div name)
    (dom/div "Loading...")))
```

### 4. Query Doesn't Match Props

If data exists but component doesn't see it, check query includes the key:
```clojure
;; State has :person/email but component doesn't query it
{:query [:person/name]}  ; ❌ won't receive :person/email

{:query [:person/name :person/email]}  ; ✅
```

---

## Further Reading

- [Fulcro Developers Guide](https://book.fulcrologic.com/)
- [EQL Specification](https://edn-query-language.org/)
