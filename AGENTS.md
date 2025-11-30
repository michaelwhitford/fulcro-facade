# AGENTS.md

Fulcro RAD application. Namespace: us.whitford.facade

Use PLAN.md for planning, CHANGELOG.md for changes. Update PLAN.md frequently.

App runs in user's browser and editor. Some issues require user intervention.

Use PLAY.md as a scratchpad

## Commands

- Restart server from cljs: (require 'development)(development/restart)
- Run tests: clojure -M:run-tests
- Lint: clj-kondo --lint .
- Check deps: clojure -M:outdated

## Expected Lint Output

2 errors, 2 warnings are intentional:

- lib/logging.clj:46-47 - Unresolved `_`, `err` - false positive from taoensso.encore/if-let
- model/hpapi.cljc - Unused UUID import - intentional test
- model/swapi.cljc - Unused UUID import - intentional test

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

## Prompt - Statechart Approach (CLJ → Browser → CLJ)

**Recommended approach using statecharts for yes/no questions:**

```clj
(require '[us.whitford.facade.model.prompt :as prompt])

;; Ask a question (returns immediately)
(def q (prompt/ask! "Deploy to production?"))
;; => {:session-id :prompt/abc123 :status :awaiting-response}

;; Poll for result
(prompt/get-result q)
;; => {:status :awaiting-response}  ; still waiting
;; => {:status :completed :answer true}  ; user clicked Yes
;; => {:status :timeout}  ; 60s elapsed with no response

;; See all pending questions
(prompt/pending-questions)
```

The browser automatically polls for questions every 5 seconds and shows toasts.
When user clicks Yes/No, the answer is sent back to CLJ and the statechart
transitions to `:ask/completed` or `:ask/timeout`.

**Benefits over inbox approach:**
- Explicit states: `:ask/idle`, `:ask/pending`, `:ask/completed`, `:ask/timeout`
- Built-in timeout support (default 60s, configurable)
- No manual inbox clearing needed
- Clean request/response correlation via session-id

## AI Workflow: Asking User Questions (Recommended)

Use the statechart-based prompt approach for confirmation questions:

```clj
(require '[us.whitford.facade.model.prompt :as prompt])

;; Step 1: Ask the question
(def q (prompt/ask! "Should I proceed with this refactoring?"))

;; Step 2: Poll for result (the browser shows toast automatically)
(prompt/get-result q)
;; => {:status :awaiting-response}  ; keep polling
;; => {:status :completed :answer true}  ; user answered!

;; Step 3: Act on response
(let [{:keys [status answer]} (prompt/get-result q)]
  (case status
    :completed (if answer
                 (println "User said YES - proceeding...")
                 (println "User said NO - aborting..."))
    :timeout   (println "User didn't respond in time")
    :awaiting-response (println "Still waiting...")))
```

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
