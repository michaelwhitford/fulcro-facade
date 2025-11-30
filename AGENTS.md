# AGENTS.md

Fulcro RAD application. See RADAR.md for runtime introspection and EQL query discovery.

Use PLAN.md for planning
Use CHANGELOG.md for changes
Update PLAN.md frequently to save progress

The app runs from the user's browser, and the user's editor. Some issues may require user intervention.

## App

- **Namespace**: `us.whitford.facade`
- **SPA (cljs running in browser)**: `us.whitford.facade.application/SPA`

## Restart

```clj
(require 'development)(development/restart)
```

## Operations

```bash
clojure -M:run-tests # kaocha tests
clj-kondo --lint . # clj-kondo lint
clojure -M:outdated # check dependencies
```

### Expected Lint Output

The linter will report 2 errors and 2 warnings - these are intentional for AI testing:

| File                    | Issue                        | Reason                                               |
| ----------------------- | ---------------------------- | ---------------------------------------------------- |
| `lib/logging.clj:46-47` | Unresolved symbol `_`, `err` | False positive from `taoensso.encore/if-let` macro   |
| `model/hpapi.cljc`      | Unused import `UUID`         | Intentional - verifies linter detects unused imports |
| `model/swapi.cljc`      | Unused import `UUID`         | Intentional - verifies linter detects unused imports |

If you see **only** these issues, linting is working correctly.

## Tests

`.cljc` files with fulcro-spec. `let` bindings outside `assertions` block, use `=>` operator.

## Martian Client Exploration

Discover available API operations at runtime:

```clj
(require '[martian.core :as martian])
(require '[us.whitford.facade.components.swapi :refer [swapi-martian]])

(martian/explore swapi-martian)           ; list operations
(martian/explore swapi-martian :people)   ; operation details + params
@(martian/response-for swapi-martian :person {:id "1"})  ; execute request
```

See MARTIAN.md for full documentation.

## See Also

| Document          | Purpose                                                             |
| ----------------- | ------------------------------------------------------------------- |
| `RADAR.md`        | Runtime introspection, EQL query discovery, client state inspection |
| `MARTIAN.md`      | HTTP client exploration, API operations, request/response debugging |
| `ARCHITECTURE.md` | System overview, component tables, data flow                        |
| `PLAN.md`         | Feature documentation and implementation details                    |
| `CHANGELOG.md`    | Version history, what changed and why                               |
