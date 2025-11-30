# STATECHARTS.md

Statecharts in this application using fulcro-statecharts.

## Overview

Statecharts are an extended finite state machine formalism that support:
- **Hierarchical states** (nested states)
- **Parallel states** (concurrent regions)
- **History** (remembering previous states)
- **Actions** (entry/exit/transition side effects)
- **Guards** (conditional transitions)
- **Delayed events** (timeouts)

## Two Runtime Environments

**IMPORTANT:** This app uses statecharts in TWO different ways:

| Environment | Integration | Purpose | Files |
|-------------|-------------|---------|-------|
| **CLJS (Browser)** | Fulcro-integrated | UI routing, client state | `client.cljs` |
| **CLJ (Server)** | Simple/standalone | Backend processing, prompt system | `components/statecharts.clj`, `model/prompt.cljc` |

### CLJS: Fulcro-Integrated Statecharts

UI routing uses `com.fulcrologic.statecharts.integration.fulcro` which:
- Lives in Fulcro app state (normalized)
- Uses actors to bind UI components
- Data paths reference `:fulcro/state-map`
- Installed via `scf/install-fulcro-statecharts!`

### CLJ: Simple Backend Statecharts

Backend statecharts use `com.fulcrologic.statecharts.simple` which:
- Standalone, no Fulcro dependency
- Working memory stored in atoms
- Event loop via `core-async-event-loop`
- Good for server-side workflows, ETL, agent communication

### Key Namespace: `runtime`

The `com.fulcrologic.statecharts.runtime` namespace provides the best helpers for inspecting statecharts:

```clj
(require '[com.fulcrologic.statecharts.runtime :as scr])

;; Get active states (set of keywords)
(scr/current-configuration env session-id)

;; Get session data model
(scr/session-data env session-id)
(scr/session-data env session-id [:specific :path])

;; Send events
(scr/send! env session-id :event-name {:data "here"})
(scr/send! env session-id :event-name {:data "here"} {:delay 5000})
```

## Self-Discovery via REPL

### Discover CLJS Routing (Browser)

```cljs
;; In CLJS REPL (shadow/repl :main)
(require '[com.fulcrologic.statecharts.integration.fulcro :as scf])
(require '[com.fulcrologic.fulcro.application :as app])
(require '[us.whitford.facade.application :refer [SPA]])

;; Get all statechart sessions and their configurations
(get (app/current-state @SPA) :com.fulcrologic.statecharts/session-id)
;; => {:com.fulcrologic.fulcro/master-statechart {...}
;;     :com.fulcrologic.statecharts.integration.fulcro.ui-routes/session {...}}

;; Extract just the active states for each session
(let [sessions (get (app/current-state @SPA) :com.fulcrologic.statecharts/session-id)]
  (into {} 
    (map (fn [[k v]] [k (:com.fulcrologic.statecharts/configuration v)]) 
         sessions)))
;; => {:com.fulcrologic.fulcro/master-statechart #{:state/initial}
;;     :com.fulcrologic.statecharts.integration.fulcro.ui-routes/session 
;;       #{:routing-info/idle :state/route-root :us.whitford.facade.ui.root/LandingPage ...}}

;; Check current routing configuration via scf helper
(scf/current-configuration @SPA :com.fulcrologic.statecharts.integration.fulcro.ui-routes/session)
```

### Discover CLJ Statecharts (Server)

```clj
;; The runtime namespace has the best inspection helpers
(require '[com.fulcrologic.statecharts.runtime :as scr])
(require '[us.whitford.facade.model.prompt :as prompt])
(require '[us.whitford.facade.components.statecharts :as sc])

;; ===== Prompt Statechart =====

;; Inspect the environment
@prompt/prompt-env
;; => Shows :statechart-registry, :working-memory-store, :event-queue

;; See all pending sessions
@prompt/pending-prompts

;; Get active states for a session (returns a set of state keywords)
(scr/current-configuration @prompt/prompt-env :prompt/some-session-id)
;; => #{:ask/pending :ask}

;; Get session data model
(scr/session-data @prompt/prompt-env :prompt/some-session-id)
;; => {:question "..." :timeout-ms 60000 :asked-at 123456789}

;; Get specific data path
(scr/session-data @prompt/prompt-env :prompt/some-session-id [:answer])
;; => true

;; ===== ETL Statechart =====

;; Access the mount state
sc/statecharts
;; => {:env {...} :running? #atom[true] :charts [...]}

;; Get active states for ETL
(scr/current-configuration (:env sc/statecharts) :swapi)
;; => #{:fetch/idle :etl/idle :swapi/running :running/etl :running/fetch :swapi}

;; Get ETL data model
(scr/session-data (:env sc/statecharts) :swapi)
;; => {:swapi {:config {...} :entities {...}} :_event nil}
```

## Statechart Locations

| Statechart | File | Runtime | Mount State |
|------------|------|---------|-------------|
| UI Routing (`application-chart`) | `client.cljs` | CLJS | N/A (app init) |
| ETL Processing | `components/statecharts.clj` | CLJ | `statechart-env` |
| Prompt (`ask-chart`) | `model/prompt.cljc` | CLJ | `prompt-statecharts` |

## Core Concepts

### States

States are the nodes in your chart. A state can be:
- **Atomic** - No children, a leaf node
- **Compound** - Has child states, one active at a time
- **Parallel** - Has child regions, all active simultaneously
- **Final** - Terminal state, chart stops when reached

```clj
(require '[com.fulcrologic.statecharts.elements :refer [state final parallel]])

;; Atomic state
(state {:id :my-state})

;; Compound state with children
(state {:id :parent :initial :child-a}
  (state {:id :child-a})
  (state {:id :child-b}))

;; Parallel regions
(parallel {:id :concurrent}
  (state {:id :region-1 :initial :r1-idle}
    (state {:id :r1-idle})
    (state {:id :r1-active}))
  (state {:id :region-2 :initial :r2-idle}
    (state {:id :r2-idle})
    (state {:id :r2-active})))

;; Final state
(final {:id :done})
```

### Transitions

Transitions move between states when events occur:

```clj
(require '[com.fulcrologic.statecharts.elements :refer [transition]])

;; Basic transition
(transition {:event :go :target :next-state})

;; Conditional transition (guard)
(transition {:event :submit 
             :cond (fn [env data] (valid? data))
             :target :success})

;; Multiple events
(transition {:event [:cancel :abort] :target :idle})

;; Internal transition (no state change, just run actions)
(transition {:event :update}
  (script {:expr (fn [env data] ...)}))
```

### Events

Events trigger transitions. Event names use dot-notation for hierarchical matching:

```clj
;; Event :error.network matches transitions for:
;; - :error.network (exact)
;; - :error.network.* (wildcard)
;; - :error (parent)
;; - :error.* (parent wildcard)

(transition {:event :error} ...)        ; catches all errors
(transition {:event :error.network} ...)  ; catches network errors only
```

### Actions (Executable Content)

Actions run during state entry, exit, or transitions:

```clj
(require '[com.fulcrologic.statecharts.elements :refer [on-entry on-exit script]])
(require '[com.fulcrologic.statecharts.data-model.operations :as ops])

(state {:id :active}
  ;; Run on entering this state
  (on-entry {}
    (script {:expr (fn [env data]
                     (println "Entered active state")
                     [(ops/assign :entered-at (System/currentTimeMillis))])}))
  
  ;; Run on exiting this state
  (on-exit {}
    (script {:expr (fn [env data]
                     (println "Leaving active state"))}))
  
  ;; Run during transition
  (transition {:event :process :target :done}
    (script {:expr (fn [env data]
                     [(ops/assign :result (process (:input data)))])})))
```

### Data Model Operations

Modify the statechart's local data:

```clj
(require '[com.fulcrologic.statecharts.data-model.operations :as ops])

;; Assign a value
(ops/assign :key value)
(ops/assign [:nested :path] value)

;; Delete a key
(ops/delete :key)
```

### Delayed Events (Timeouts)

Send events after a delay:

```clj
(require '[com.fulcrologic.statecharts.elements :refer [Send]])
(require '[com.fulcrologic.statecharts.convenience :refer [send-after]])

;; Send event after delay
(on-entry {}
  (Send {:id :timeout-id
         :event :timeout
         :delay 5000}))  ; 5 seconds

;; Cancel if we exit before it fires
(on-exit {}
  (cancel {:sendid :timeout-id}))

;; Or use the convenience helper that does both:
(send-after {:id :my-timeout
             :event :timeout
             :delay 5000})
```

### Dynamic Delays

Use `:delayexpr` for dynamic timeout values:

```clj
(Send {:id :timeout-send
       :event :timeout
       :delayexpr (fn [env data] (:timeout-ms data))})
```

## CLJ: Simple Backend Statecharts

For server-side statecharts (no Fulcro integration). Used by:
- `model/prompt.cljc` - Prompt/question system
- `components/statecharts.clj` - ETL processing

```clj
(require '[com.fulcrologic.statecharts.simple :as simple])
(require '[com.fulcrologic.statecharts.runtime :as scr])
(require '[com.fulcrologic.statecharts.chart :refer [statechart]])
(require '[com.fulcrologic.statecharts.event-queue.core-async-event-loop :as scloop])

;; 1. Create environment
(def env (simple/simple-env {}))

;; 2. Start event loop (required for delayed events)
(def running? (scloop/run-event-loop! env 100))

;; 3. Register chart
(simple/register! env :my-chart my-statechart)

;; 4. Start a session
(simple/start! env :my-chart :session-1)

;; 5. Send events
(simple/send! env {:target :session-1
                   :event :my-event
                   :data {:foo "bar"}})

;; 6. Send delayed events
(simple/send! env {:target :session-1
                   :event :timeout
                   :delay 5000})

;; Inspect session state - use runtime namespace
(scr/current-configuration env :session-1)
;; => #{:my-state :parent-state}

(scr/session-data env :session-1)
;; => {:foo "bar" ...}

;; Stop event loop
(reset! running? false)
```

### Mount State Pattern (Recommended)

Wrap the environment in a mount state for lifecycle management:

```clj
(require '[mount.core :refer [defstate]])

(defstate my-statecharts
  :start
  (let [env (simple/simple-env {})
        running? (scloop/run-event-loop! env 100)]
    (simple/register! env :my-chart my-statechart)
    {:env env :running? running?})
  :stop
  (reset! (:running? my-statecharts) false))
```

## CLJS: Fulcro-Integrated Statecharts

For browser statecharts integrated with Fulcro app state. Used by:
- `client.cljs` - UI routing via `application-chart`

```cljs
(require '[com.fulcrologic.statecharts.integration.fulcro :as scf])

;; Install on app (once at startup, in client.cljs init)
(scf/install-fulcro-statecharts! app)

;; Register a chart
(scf/register-statechart! app :my-chart my-statechart)

;; Start a session
(scf/start! app {:machine :my-chart
                 :session-id :my-session
                 :data {:initial "data"}})

;; Send events
(scf/send! app :my-session :my-event {:optional "data"})

;; Check current states
(scf/current-configuration app :my-session)
;; => #{:state/running :state/substateA}
```

### Fulcro Data Model Paths

In Fulcro-integrated charts, data paths have special prefixes:

```clj
;; Local to statechart session
[:local-key]
[:ROOT :local-key]  ; same as above

;; Fulcro app state (global)
[:fulcro/state-map :some :path]
[:fulcro/state :some :path]  ; alias

;; Actor (UI component) data
[:actor/form :field-name]  ; resolves actor's ident
```

### Actors and Aliases

```clj
;; Define aliases for cleaner code
{:fulcro/aliases {:user-name [:actor/form :user/name]
                  :loading?  [:fulcro/state-map :ui/loading?]}}

;; Define actors (UI component references)
{:fulcro/actors {:actor/form (scf/actor UserForm [:user/id 123])}}

;; Use in expressions
(script {:expr (fn [env data]
                 (let [{:keys [user-name loading?]} (scf/resolve-aliases data)]
                   ...))})
```

## Application Examples

### 1. CLJS: UI Routing (client.cljs)

Browser-side routing using Fulcro RAD's statechart integration:

```cljs
(def application-chart
  (statechart {:name "fulcro-swapi"}
    (uir/routing-regions
      (uir/routes {:id :region/routes
                   :routing/root Root}
        (state {:id :state/running}
          (uir/rstate {:route/target `LandingPage
                       :route/path   ["landing-page"]})
          (ri/report-state {:route/target `PersonList
                            :route/path   ["people"]})
          (ri/form-state {:route/target `PersonForm
                          :route/path   ["person"]}))))))

;; Explore in CLJS REPL:
;; (require '[us.whitford.facade.client :as client])
;; client/application-chart
```

### 2. CLJ: Backend ETL (components/statecharts.clj)

Server-side data processing with parallel regions:

```clj
(def swapi-statechart
  (statechart {:name "swapi" :initial :swapi}
    (state {:id :swapi :initial :swapi/start}
      (transition {:event :exit :target :swapi/end})
      
      (state {:id :swapi/start}
        (on-entry {}
          (script {:expr (fn [env data]
                           [(ops/assign [:swapi :config] cfg)])}))
        (transition {:target :swapi/running}))
      
      (parallel {:id :swapi/running}
        (state {:id :running/fetch :initial :fetch/idle}
          (state {:id :fetch/idle})
          (state {:id :fetch/processing}
            (invoke {:type :future
                     :src (fn [params] (fetch-data params))})))
        
        (state {:id :running/etl :initial :etl/idle}
          (state {:id :etl/idle}
            (transition {:event :etl :target :etl/processing}))
          (state {:id :etl/processing}
            (transition {:target :etl/idle}))))
      
      (final {:id :swapi/end}))))

;; Explore in CLJ REPL:
;; (require '[us.whitford.facade.components.statecharts :as sc])
;; sc/swapi-statechart
;; @sc/statechart-env
```

### 3. CLJ: Prompt (model/prompt.cljc)

Server-side statechart for CLJ↔Browser question/answer flow:

```clj
(def ask-statechart
  (statechart {:id :ask-chart}
    (state {:id :ask :initial :ask/idle}
      
      (state {:id :ask/idle}
        (transition {:event :event/ask :target :ask/pending}
          (script {:expr (fn [env {:keys [_event]}]
                           [(ops/assign :question (:question (:data _event)))
                            (ops/assign :timeout-ms (:timeout-ms (:data _event)))])})))
      
      (state {:id :ask/pending}
        (on-entry {}
          (Send {:id :timeout-send
                 :event :event/timeout
                 :delayexpr (fn [env data] (:timeout-ms data))}))
        (transition {:event :event/answer :target :ask/completed}
          (script {:expr (fn [env {:keys [_event]}]
                           [(ops/assign :answer (:answer (:data _event)))])}))
        (transition {:event :event/timeout :target :ask/timeout})
        (transition {:event :event/cancel :target :ask/cancelled}))
      
      (final {:id :ask/completed})
      (final {:id :ask/timeout})
      (final {:id :ask/cancelled}))))

;; Explore in CLJ REPL:
;; (require '[us.whitford.facade.model.prompt :as prompt])
;; prompt/ask-statechart       ;; Chart definition
;; @prompt/prompt-env          ;; Runtime environment
;; @prompt/pending-prompts     ;; All sessions
;; (prompt/pending-questions)  ;; Only awaiting-response
```

## Convenience Helpers

```clj
(require '[com.fulcrologic.statecharts.convenience :refer [on handle choice send-after]])

;; Shorthand for transition with target
(on :event/click :next-state)
;; Expands to: (transition {:event :event/click :target :next-state})

;; Shorthand for event handler (no state change)
(handle :event/update my-handler-fn)
;; Expands to: (transition {:event :event/update} (script {:expr my-handler-fn}))

;; Choice state (conditional routing)
(choice {:id :check-result}
  valid?    :success
  :else     :failure)
;; Expands to eventless transitions with guards

;; Delayed event with auto-cancel on exit
(send-after {:id :my-timeout :event :timeout :delay 5000})
;; Expands to on-entry Send + on-exit cancel
```

## Testing Statecharts

```clj
(require '[com.fulcrologic.statecharts.testing :as testing])

(defn test-env []
  (testing/new-testing-env {:statechart my-chart} {}))

(let [env (test-env)]
  ;; Set up initial state
  (testing/goto-configuration! env [] #{:some/leaf-state})
  
  ;; Run events
  (testing/run-events! env :event/something)
  
  ;; Assert on configuration
  (is (testing/in-state? env :expected/state)))
```

## Debugging & Self-Discovery

### CLJ: Exploring Backend Statecharts

```clj
;; Use the runtime namespace for clean inspection
(require '[com.fulcrologic.statecharts.runtime :as scr])
(require '[us.whitford.facade.model.prompt :as prompt])
(require '[us.whitford.facade.components.statecharts :as sc])

;; View the environment (registry, event-queue, working-memory-store)
@prompt/prompt-env

;; List all session IDs
(keys @prompt/pending-prompts)

;; Get active states for a session
(scr/current-configuration @prompt/prompt-env :prompt/some-uuid)
;; => #{:ask/pending :ask}

;; Get session data model
(scr/session-data @prompt/prompt-env :prompt/some-uuid)
;; => {:question "..." :timeout-ms 60000 :asked-at 123456789}

;; Quick health check for all statecharts
{:prompt-env-running? (boolean @prompt/prompt-env)
 :pending-questions (count (prompt/pending-questions))
 :etl-states (scr/current-configuration (:env sc/statecharts) :swapi)}

;; If you need raw working memory (contains internal keys too):
(require '[com.fulcrologic.statecharts.protocols :as sp])
(let [store (:com.fulcrologic.statecharts/working-memory-store @prompt/prompt-env)]
  (sp/get-working-memory store @prompt/prompt-env :prompt/some-uuid))
;; Working memory contains:
;; - :com.fulcrologic.statecharts/configuration - Set of active state IDs
;; - :com.fulcrologic.statecharts/history-value - History state memory
;; - :com.fulcrologic.statecharts.data-model.working-memory-data-model/data-model - User data
```

### CLJS: Exploring Fulcro-Integrated Statecharts

```cljs
;; In CLJS REPL: (shadow/repl :main)
(require '[com.fulcrologic.statecharts.integration.fulcro :as scf])
(require '[com.fulcrologic.fulcro.application :as app])
(require '[us.whitford.facade.application :refer [SPA]])

;; Get all sessions with their active states
(let [sessions (get (app/current-state @SPA) :com.fulcrologic.statecharts/session-id)]
  (into {} 
    (map (fn [[k v]] [k (:com.fulcrologic.statecharts/configuration v)]) 
         sessions)))

;; Check current routing configuration
(scf/current-configuration @SPA :com.fulcrologic.statecharts.integration.fulcro.ui-routes/session)

;; Access full statechart state in Fulcro app-state
(get (app/current-state @SPA) :com.fulcrologic.statecharts/session-id)
```

### Add Logging to States

```clj
(on-entry {}
  (script {:expr (fn [env data]
                   (tap> {:entering :my-state :data data})
                   [])}))
```

### Inspect Chart Definition

```clj
;; Charts are data - you can inspect them
(require '[clojure.pprint :refer [pprint]])
(pprint (get-in @prompt/prompt-env 
                [:com.fulcrologic.statecharts/statechart-registry :charts :ask-chart]))
```

## Common Patterns

### Request/Response Flow

```clj
(state {:id :idle}
  (transition {:event :request :target :loading}))

(state {:id :loading}
  (on-entry {}
    (script {:expr (fn [env data] (start-request! data))}))
  (transition {:event :success :target :idle}
    (script {:expr (fn [env {:keys [_event]}]
                     [(ops/assign :result (:data _event))])}))
  (transition {:event :error :target :error}))

(state {:id :error}
  (transition {:event :retry :target :loading})
  (transition {:event :dismiss :target :idle}))
```

### Timeout with Retry

```clj
(state {:id :waiting}
  (send-after {:id :timeout :event :timeout :delay 30000})
  (transition {:event :response :target :success})
  (transition {:event :timeout :target :retry-or-fail}))

(state {:id :retry-or-fail}
  (transition {:cond (fn [_ data] (< (:retries data 0) 3))
               :target :waiting}
    (script {:expr (fn [_ data]
                     [(ops/assign :retries (inc (:retries data 0)))])}))
  (transition {:target :failed}))
```

## References

- [SCXML W3C Specification](https://www.w3.org/TR/scxml/) - The standard this library implements
- [fulcro-statecharts Guide](file:///Users/mwhitford/src/statecharts/Guide.adoc) - Full documentation
- [Statecharts.dev](https://statecharts.dev/) - General statechart concepts
