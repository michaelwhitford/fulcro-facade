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

## Agent ↔ User Communication

**Notifications (fire-and-forget):**
```cljs
(require '[us.whitford.facade.ui.toast :refer [toast!]])
(toast! "Task complete! 🤖")
```

**Yes/No Questions (with response):**
```clj
(require '[us.whitford.facade.model.prompt :as prompt])

(def q (prompt/ask! "Deploy to production?"))

;; Poll until answered (browser shows toast automatically)
(prompt/get-result q)
;; => {:status :awaiting-response}  ; keep polling
;; => {:status :completed :answer true}  ; user clicked Yes/No
;; => {:status :timeout}  ; 60s elapsed
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
