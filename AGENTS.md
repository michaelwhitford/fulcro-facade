# AGENTS.md

Fulcro RAD application. Namespace: us.whitford.facade

Planning: PLAN.md | Changes: CHANGELOG.md | Scratchpad: PLAY.md

## AI Quickstart

```clj
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))

(-> (p {} [:radar/overview]) :radar/overview :radar/summary)
;; => "Mount: 15 states, 163 attrs, 16 entities, 12 forms, 11 reports, 15 refs"

(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :root (map :output))

(p {} [{:swapi/all-people [:total {:results [:person/name]}]}])
```

## Commands

| Task | Command |
|------|---------|
| Restart server | `(require 'development)(development/restart)` |
| Run tests | `clojure -M:run-tests` |
| Lint | `clj-kondo --lint .` |

## Agent ↔ User Communication

```clj
;; Notification (fire-and-forget)
(require '[us.whitford.facade.ui.toast :refer [toast!]])
(toast! "Task complete! 🤖")

;; Yes/No question (poll for answer)
(require '[us.whitford.facade.model.prompt :as prompt])
(def q (prompt/ask! "Deploy?"))
(prompt/get-result q)  ;; {:status :completed :answer true}
```

## Documentation

- QUICK_REFERENCE.md - Essential patterns
- INTEGRATION_GUIDE.md - Adding new APIs
- TROUBLESHOOTING.md - Common issues
- RADAR.md - Runtime introspection
- ARCHITECTURE.md - System overview
