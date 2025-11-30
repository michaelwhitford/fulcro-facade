# AGENTS.md

Fulcro RAD application. See RADAR.md for runtime introspection and EQL query discovery.

Use PLAN.md for planning
Use CHANGELOG.md for changes
Update PLAN.md as you go to save progress

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

## Tests

`.cljc` files with fulcro-spec. `let` bindings outside `assertions` block, use `=>` operator.
