# Architecture

## Overview

Facade is a Fulcro RAD application that serves as a client to multiple backend APIs.

## Discover via REPL

```clj
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))
(-> (p {} [:radar/overview]) :radar/overview :radar/summary)
```

See **AGENTS.md** for complete discovery patterns.

## Configuration

- `config/defaults.edn` — Base configuration
- `config/dev.edn` — Development overrides
- `config/prod.edn` — Production overrides

Hierarchical merging: defaults ← env-specific

## Hot Reload

- **Client (CLJS)**: Hot-reloads automatically via shadow-cljs
- **Server (CLJ)**: Requires `(development/restart)` from CLJ REPL

## Query Param Normalization

The parser includes middleware that normalizes query-params, allowing resolvers to use simple keywords even when RAD sends namespaced params:

```clojure
;; RAD sends: {:us.whitford.facade.ui.search-forms/search-term "luke"}
;; Resolver receives both original AND: {:search-term "luke"}

;; So resolvers can destructure simply:
(let [{:keys [search-term]} query-params] ...)
```

## Agent Communication

Two mechanisms for CLJ REPL ↔ Browser communication:

### Toast Notifications (CLJS, fire-and-forget)

```clojure
;; From CLJS REPL - displays toast in browser
(require '[us.whitford.facade.ui.toast :refer [toast!]])
(toast! "Task complete! 🤖")
```

### Statechart Prompts (CLJ → Browser → CLJ)

```clojure
;; Ask a yes/no question from CLJ REPL
(require '[us.whitford.facade.model.prompt :as prompt])
(def q (prompt/ask! "Deploy to production?"))
;; => {:session-id :prompt/abc123 :status :awaiting-response}

;; Poll for answer (user sees toast in browser)
(prompt/get-result q)
;; => {:status :completed :answer true}
```

### Agent Messages (CLJS → CLJ inbox)

```clojure
;; From CLJS - send message to CLJ inbox
(comp/transact! @SPA [(agent-comms/send-message {:message "hello" :data {:foo 1}})])

;; From CLJ - read inbox
@us.whitford.facade.model.agent-comms/inbox
```
