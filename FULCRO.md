# Fulcro Framework Guide

> **Purpose**: This document explains core Fulcro concepts used in this project.
> **Audience**: AI agents and developers new to Fulcro.
> **Scope**: Core Fulcro features. See FULCRO-RAD.md for RAD extensions.

---

## Table of Contents

1. [What is Fulcro?](#what-is-fulcro)
2. [Normalization and Idents](#normalization-and-idents)
3. [EQL Queries](#eql-queries)
4. [Component Types](#component-types)
5. [State Management](#state-management)
6. [Transactions and Mutations](#transactions-and-mutations)
7. [Component Lifecycle](#component-lifecycle)
8. [Common Patterns](#common-patterns)
9. [Troubleshooting](#troubleshooting)

---

## What is Fulcro?

**Fulcro** is a full-stack web framework for Clojure/ClojureScript that provides:

- **Normalized graph database** for client state (like Redux + normalizr)
- **Declarative data fetching** via EQL queries
- **Component-local queries** that compose into global queries
- **Optimistic updates** with automatic rollback on error
- **Server-side rendering** support
- **Type-safe** via Clojure spec

### Philosophy

Fulcro follows these principles:

1. **Data is immutable** - State changes create new versions
2. **Queries co-locate with UI** - Components declare what data they need
3. **Normalization by default** - No duplicate data in app state
4. **Server integration** - Query backend with same EQL language

### When to Use Fulcro

✅ **Use Fulcro when**:

- Building SPAs with complex data requirements
- Need normalized state (avoid data duplication)
- Want declarative data fetching
- Need server-side rendering

❌ **Consider alternatives when**:

- Building simple static sites
- Don't need normalized state
- Already heavily invested in another framework

---

## Normalization and Idents

Fulcro stores app state as a **normalized graph database**. Understanding this is crucial.

### The Problem: Denormalized Data

**Without normalization**, data gets duplicated:

```clojure
;; User appears in multiple places:
{:app/current-user {:user/id 1 :user/name "Alice" :user/email "alice@example.com"}
 :app/messages [{:message/id 1
                 :message/text "Hello"
                 :message/author {:user/id 1 :user/name "Alice" :user/email "alice@example.com"}}
                {:message/id 2
                 :message/text "World"
                 :message/author {:user/id 1 :user/name "Alice" :user/email "alice@example.com"}}]}
```

**Problems**:

- Alice's data duplicated 3 times
- Update email → must update in 3 places
- Easy to have inconsistent data
- Memory inefficient

### The Solution: Normalized Graph

**With normalization**, data stored once, referenced by ident:

```clojure
{;; Normalized tables (like a database)
 :user/id {1 {:user/id 1
              :user/name "Alice"
              :user/email "alice@example.com"}}

 :message/id {1 {:message/id 1
                 :message/text "Hello"
                 :message/author [:user/id 1]}    ; ⭐ Ident reference
              2 {:message/id 2
                 :message/text "World"
                 :message/author [:user/id 1]}}   ; ⭐ Same reference

 ;; App root points to data via idents
 :app/current-user [:user/id 1]                   ; ⭐ Ident
 :app/messages [[:message/id 1]                   ; ⭐ Ident
                [:message/id 2]]}                 ; ⭐ Ident
```

**Benefits**:

- Alice's data stored **once**
- Update email → change in **one** place
- All references automatically see the update
- Memory efficient

### What is an Ident?

An **ident** is a tuple `[<entity-type> <entity-id>]` that uniquely identifies an entity:

```clojure
[:user/id 1]       ; User with ID 1
[:message/id 42]   ; Message with ID 42
[:person/id "person-1"]  ; Person with ID "person-1"
```

**Components**:

1. **Entity type** - Keyword (usually the identity attribute)
2. **Entity ID** - Unique value (string, int, UUID)

### How Fulcro Normalizes

When you define a component with an `:ident`, Fulcro automatically normalizes:

```clojure
(defsc User [this props]
  {:query [:user/id :user/name :user/email]
   :ident :user/id}                              ; ⭐ Shorthand for [:user/id (:user/id props)]

  (dom/div (:user/name props)))
```

**Loading this data**:

```clojure
;; Server returns:
{:user/id 1 :user/name "Alice" :user/email "alice@example.com"}

;; Fulcro normalizes to:
{:user/id {1 {:user/id 1 :user/name "Alice" :user/email "alice@example.com"}}}
```

### Ident Forms

#### Keyword Shorthand (Recommended)

```clojure
:ident :user/id
;; Equivalent to: [:user/id (:user/id props)]
```

#### Function Form (Advanced)

```clojure
:ident (fn [this props] [:user/id (:user/id props)])
```

⚠️ **Important**: Use keyword shorthand unless you have special needs. The function form has limitations (runs during normalization before props are fully available).

---

## EQL Queries

**EQL** (EDN Query Language) is how Fulcro components declare what data they need.

### Basic Query Syntax

#### Simple Properties

```clojure
[:user/id :user/name :user/email]
```

Fetches three properties of a user.

#### Joins (Nested Data)

```clojure
[:user/id
 :user/name
 {:user/messages [:message/id :message/text]}]  ; ⭐ Join to messages
```

Fetches user with nested messages.

#### Parameterized Queries

```clojure
[(:users/search {:search-term "alice"})]         ; ⭐ Pass parameters
```

Pass parameters to resolvers.

#### Ident Queries (Lookup by ID)

```clojure
[{[:user/id 1] [:user/name :user/email]}]        ; ⭐ Fetch specific user
```

Fetch a specific entity by ident.

### Query Composition

Fulcro queries **compose**. Child component queries are automatically included in parent queries.

```clojure
;; Child component
(defsc Message [this {:message/keys [id text]}]
  {:query [:message/id :message/text]            ; ⭐ Child query
   :ident :message/id}
  (dom/div text))

;; Parent component
(defsc MessageList [this {:keys [messages]}]
  {:query [{:messages (comp/get-query Message)}] ; ⭐ Include child query
   :ident :list/id}
  (dom/div
    (map ui-message messages)))

;; Composed query automatically becomes:
;; [{:messages [:message/id :message/text]}]
```

### Link Queries

For **references** (idents), use link queries:

```clojure
(defsc Person [this props]
  {:query [:person/id :person/name
           {:person/homeworld (comp/get-query Planet)}]  ; ⭐ Link to Planet
   :ident :person/id}
  ...)

(defsc Planet [this props]
  {:query [:planet/id :planet/name]
   :ident :planet/id}
  ...)

;; Query result:
{:person/id "1"
 :person/name "Luke"
 :person/homeworld {:planet/id "1" :planet/name "Tatooine"}}

;; Normalized as:
{:person/id {"1" {:person/id "1"
                  :person/name "Luke"
                  :person/homeworld [:planet/id "1"]}}  ; ⭐ Ident reference
 :planet/id {"1" {:planet/id "1" :planet/name "Tatooine"}}}
```

---

## Component Types

Fulcro has different component types for different use cases.

### Regular Components (defsc)

**For custom UI** without CRUD needs:

```clojure
(defsc Banner [this {:keys [message]}]
  {:query [:banner/message]
   :ident :banner/id}

  (dom/div :.banner message))
```

**When to use**:

- Custom layouts
- Stateless presentations
- Non-CRUD UI

### Form Components (defsc-form)

**For editing entities** with save/cancel:

```clojure
(form/defsc-form PersonForm [this props]
  {fo/id person_id
   fo/attributes [person_id person_name]
   fo/route-prefix "person"}

  ;; RAD auto-generates form UI
  )
```

**When to use**:

- Creating/editing entities
- Need validation
- Need save/cancel
- Want automatic form state management

See FULCRO-RAD.md for details.

### Report Components (defsc-report)

**For data tables** with filtering/sorting:

```clojure
(report/defsc-report PersonList [this props]
  {ro/source-attribute all-people
   ro/columns [:person/name :person/height]
   ro/row-pk :person/id}

  ;; RAD auto-generates table UI
  )
```

**When to use**:

- Showing lists of data
- Need filtering/sorting/pagination
- CRUD list views

See FULCRO-RAD.md for details.

### Decision Matrix

| Need                  | Use            |
| --------------------- | -------------- |
| Custom UI             | `defsc`        |
| Edit single entity    | `defsc-form`   |
| Show list of entities | `defsc-report` |
| Simple presentation   | `defsc`        |

---

## State Management

Fulcro's state is an **atom** containing a normalized map.

### App State Structure

```clojure
;; Access app state
(require '[com.fulcrologic.fulcro.application :as app])
(require '[us.whitford.facade.application :refer [SPA]])

@(::app/state-atom @SPA)

;; Structure:
{;; Normalized entity tables
 :user/id {1 {:user/id 1 :user/name "Alice"}}
 :message/id {1 {:message/id 1 :message/text "Hello"}}

 ;; Root data (what UI sees initially)
 :root/current-user [:user/id 1]
 :root/messages [[:message/id 1]]

 ;; Component-local state
 ::some-component {[:component/id "x"] {:ui/expanded? true}}

 ;; UI state machines (RAD forms/reports)
 :com.fulcrologic.fulcro.ui-state-machines/asm-id {...}}
```

### Reading State

#### In Components (Preferred)

```clojure
(defsc User [this {:user/keys [name email]}]  ; ⭐ Props from query
  {:query [:user/name :user/email]}
  (dom/div name))
```

#### Directly from State Atom (Rare)

```clojure
;; In REPL or advanced cases
(let [state @(::app/state-atom @SPA)
      user (get-in state [:user/id 1])]
  (:user/name user))
```

### Updating State

#### Via Mutations (Preferred)

```clojure
(defmutation update-user-name [{:user/keys [id name]}]
  (action [{:keys [state]}]
    (swap! state assoc-in [:user/id id :user/name] name)))

;; Call from component:
(comp/transact! this [(update-user-name {:user/id 1 :name "Bob"})])
```

#### Directly (Rare)

```clojure
;; Only in REPL or tests
(swap! (::app/state-atom @SPA)
  assoc-in [:user/id 1 :user/name] "Bob")
```

---

## Transactions and Mutations

**Transactions** change app state. **Mutations** are the operations in transactions.

### Defining Mutations

```clojure
(defmutation update-email [{:user/keys [id email]}]
  (action [{:keys [state]}]                      ; ⭐ Local state update
    (swap! state assoc-in [:user/id id :user/email] email))

  (remote [env]                                  ; ⭐ Optional: send to server
    true))
```

### Calling Mutations

```clojure
;; In a component:
(dom/button
  {:onClick #(comp/transact! this
               [(update-email {:user/id 1 :email "newemail@example.com"})])}
  "Update Email")
```

### Optimistic Updates

Mutations run **optimistically** (update UI immediately):

```clojure
(defmutation save-user [params]
  (action [{:keys [state]}]
    ;; Update UI immediately
    (swap! state assoc-in [:user/id 1 :ui/saving?] true))

  (remote [env] true)                            ; Send to server

  (ok-action [env]                               ; ⭐ On success
    (swap! (:state env) assoc-in [:user/id 1 :ui/saving?] false))

  (error-action [env]                            ; ⭐ On error
    (swap! (:state env) assoc-in [:user/id 1 :ui/saving?] false)
    ;; Fulcro auto-rolls back the optimistic update
    ))
```

---

## Component Lifecycle

Fulcro components have a lifecycle similar to React:

```
Initialize
    ↓
Query for Data
    ↓
Render (with props from query)
    ↓
User Interaction
    ↓
Mutation → Update State
    ↓
Re-render (with new props)
    ↓
Unmount
```

### Lifecycle Methods

```clojure
(defsc User [this props]
  {:query [:user/id :user/name]
   :ident :user/id

   ;; Called once when component mounts
   :componentDidMount
   (fn [this]
     (comp/transact! this [(load-user-data)]))

   ;; Called before component unmounts
   :componentWillUnmount
   (fn [this]
     (cleanup-resources))}

  (dom/div (:user/name props)))
```

---

## Common Patterns

### Pattern: Master-Detail View

**List of items with detail panel**:

```clojure
;; Detail component
(defsc PersonDetail [this {:person/keys [id name height]}]
  {:query [:person/id :person/name :person/height]
   :ident :person/id}
  (dom/div name))

;; List component
(defsc PersonList [this {:keys [people selected-person]}]
  {:query [{:people (comp/get-query PersonDetail)}
           {:selected-person (comp/get-query PersonDetail)}]
   :ident :list/id}

  (dom/div
    ;; List
    (dom/ul
      (map (fn [person]
             (dom/li
               {:onClick #(comp/transact! this
                            [(select-person {:person/id (:person/id person)})])}
               (:person/name person)))
           people))

    ;; Detail
    (when selected-person
      (ui-person-detail selected-person))))
```

### Pattern: Lazy Loading

**Load data on demand**:

```clojure
(defsc PersonDetail [this props]
  {:query [:person/id :person/name :ui/loaded?]
   :ident :person/id

   :componentDidMount
   (fn [this]
     (when-not (:ui/loaded? (comp/props this))
       (df/load! this [:person/id (:person/id (comp/props this))]
                 PersonDetail)))}

  (if (:ui/loaded? props)
    (dom/div (:person/name props))
    (dom/div "Loading...")))
```

### Pattern: Component-Local State

**State that doesn't need to be in the graph**:

```clojure
(defsc Accordion [this {:keys [ui/expanded?]}]
  {:query [:ui/expanded?]                        ; ⭐ ui/ namespace = local state
   :ident :accordion/id

   :initial-state {:ui/expanded? false}}         ; ⭐ Initial value

  (dom/div
    (dom/button
      {:onClick #(m/toggle! this :ui/expanded?)} ; ⭐ Built-in toggle
      "Toggle")

    (when expanded?
      (dom/div "Content"))))
```

---

## Troubleshooting

### "Component has no Ident"

**Problem**: Component queries data but it's not normalized.

**Solution**: Add `:ident` to component:

```clojure
(defsc Person [this props]
  {:query [:person/id :person/name]
   :ident :person/id}                            ; ⭐ Add this
  ...)
```

### "Cannot Read Property of Undefined"

**Problem**: Component renders before data is loaded.

**Solution**: Add nil check:

```clojure
(defsc Person [this {:person/keys [name]}]
  {:query [:person/name]}

  (if name                                       ; ⭐ Check for data
    (dom/div name)
    (dom/div "Loading...")))
```

### Ident Must Be Keyword

**Problem**: Using function form for ident incorrectly.

**Wrong**:

```clojure
:ident (fn [] [:person/id (:person/id props)])   ; ❌ props not available
```

**Right**:

```clojure
:ident :person/id                                ; ✅ Use shorthand
```

### Data Not Updating in UI

**Problem**: State changes but UI doesn't re-render.

**Common Causes**:

1. **Mutating state directly** instead of using mutations
2. **Not refreshing** after mutation
3. **Query doesn't include** the changed attribute

**Solution**:

```clojure
(defmutation update-name [params]
  (action [{:keys [state]}]
    (swap! state ...))

  (refresh [env]                                 ; ⭐ Tell Fulcro what to refresh
    [:person/name]))
```

---

## Next Steps

- See **FULCRO-RAD.md** for RAD-specific features (forms, reports)
- See **PATHOM.md** for understanding resolvers and data fetching
- See **QUICK_REFERENCE.md** for project-specific patterns
- See **ARCHITECTURE.md** for how components fit together in this app

---

## Further Reading

- [Fulcro Developers Guide](https://book.fulcrologic.com/)
- [EQL Specification](https://edn-query-language.org/)
- [Fulcro Community](https://clojurians.slack.com) (#fulcro channel)
