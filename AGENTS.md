# AGENTS.md

Fulcro RAD application. Namespace: us.whitford.facade

Use PLAN.md for planning, CHANGELOG.md for changes. Update PLAN.md frequently.

App runs in user's browser and editor. Some issues require user intervention.

Use PLAY.md as a scratchpad

## Commands

Restart server: (require 'development)(development/restart)
Run tests: clojure -M:run-tests
Lint: clj-kondo --lint .
Check deps: clojure -M:outdated

## Expected Lint Output

2 errors, 2 warnings are intentional:

- lib/logging.clj:46-47 - Unresolved `_`, `err` - false positive from taoensso.encore/if-let
- model/hpapi.cljc - Unused UUID import - intentional test
- model/swapi.cljc - Unused UUID import - intentional test

## Tests

.cljc files with fulcro-spec. let bindings outside assertions block, use => operator.

## Martian Client

```clj
(require '[martian.core :as martian])
(require '[us.whitford.facade.components.swapi :refer [swapi-martian]])
(martian/explore swapi-martian)
(martian/explore swapi-martian :people)
@(martian/response-for swapi-martian :person {:id "1"})
```

## Adding APIs

See INTEGRATION_GUIDE.md. Examples by complexity:

- ipapi.\* - Simple: single entity
- hpapi.\* - Medium: multiple entities, filtering
- swapi.\* - Complex: relationships, pagination

Files for new API (5 layers):

1. src/main/<api>.yml - OpenAPI spec
2. components/<api>.clj - Martian client
3. model/<api>.cljc - Resolvers
4. model_rad/<api>.cljc - RAD attributes
5. ui/<api>\_forms.cljc - UI

Also update: config/defaults.edn, model_rad/attributes.cljc, components/parser.clj, ui/root.cljc, client.cljs

## Toasts (CLJS REPL)

Send notifications to the user's browser:

```cljs
(require '[us.whitford.facade.ui.toast :refer [toast! ask!]])
(toast! "Hello from the AI agent! 🤖")
(toast! {:position "bottom-center" :autoClose 3000} "Task complete!")
```

Options: :position (top-right, bottom-center, etc.), :autoClose (ms)

### Ask Yes/No Questions

```cljs
(ask! "Continue with deployment?")
```

Then read the answer in CLJ:

```clj
(last @us.whitford.facade.model.agent-comms/inbox)
;; => {:message "ANSWER", :data {:question "..." :answer true}, ...}
```

## Agent Communication (Browser → CLJ)

Send messages from browser back to CLJ REPL:

```cljs
(require '[us.whitford.facade.model.agent-comms :as agent])
(require '[us.whitford.facade.application :refer [SPA]])
(comp/transact! @SPA [(agent/send-message {:message "Hello" :data {:foo 1}})])
```

Read messages in CLJ:

```clj
(require '[us.whitford.facade.model.agent-comms :refer [inbox]])
@inbox  ;; vector of {:message ... :data ... :timestamp ...}
(reset! inbox [])  ;; clear
```

Example: See `ui/game.cljc` (Whack-a-Toast!) for toast callbacks sending game results to server.

## AI Agent Workflow: Asking User Questions

When you need user confirmation or input, follow this pattern:

**Step 1: Clear inbox and note message count (CLJ)**
```clj
(require '[us.whitford.facade.model.agent-comms :refer [inbox]])
(reset! inbox [])
```

**Step 2: Send question to browser (CLJS)**
```cljs
(require '[us.whitford.facade.ui.toast :refer [ask!]])
(ask! "Should I proceed with this refactoring?")
```

**Step 3: Poll for answer (CLJ)**
```clj
;; Check inbox - user's answer will appear when they click Yes/No
@inbox
;; => [{:message "ANSWER", :data {:question "..." :answer true}, :timestamp #inst "..."}]
```

**Step 4: Act on response (CLJ)**
```clj
(if (:answer (:data (last @inbox)))
  (println "User said YES - proceeding...")
  (println "User said NO - aborting..."))
```

**Important:** The user must click Yes or No on the toast in their browser. 
Poll `@inbox` until the answer arrives. Messages have `:message "ANSWER"` 
and `:data {:question "..." :answer true/false}`.

## Documentation

- QUICK_REFERENCE.md - Essential patterns and commands
- INTEGRATION_GUIDE.md - Adding new APIs
- TROUBLESHOOTING.md - Common issues
- RADAR.md - Runtime introspection, EQL queries
- MARTIAN.md - HTTP client exploration
- ARCHITECTURE.md - System overview
- FULCRO-RAD.md - RAD forms/reports concepts
- PATHOM.md - Resolver concepts
- FULCRO.md - App structure, normalization, idents
