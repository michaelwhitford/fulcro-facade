# AGENTS.md

project checked out at: /Users/mwhitford/src/facade

## Overview

Facade provides a client for multiple backend apis using fulcro and fulcro-rad.

Use a single PLAN.md for planning. Use a single CHANGELOG.md for changes. Do not create summary documents.

The clj-nrepl-eval tool should have access to both a clj and cljs nrepl for this project, use them as needed to fulfill user requests.
The app should have a clj and cljs repl started from the editor.

App restarts can be accomplished from the repl:

```clojure
(require 'development)
(development/restart)
```

## Diagnostic Tools & Docmentation

**ALWAYS start troubleshooting with RADAR**

This query returns useful fulcro-rad diagnostic data

```clojure
(us.whitford.facade.components.parser {} [:radar/overview])
```

This query returns a large amount of data from the pathom-env used by resolvers.

```clojure
(us.whitford.facade.components.parser {} [:radar/pathom-env])
```

## Build & Test Commands

- **Lint and Run tests:** `clj-kondo --lint . && clojure -M:run-tests`
- **Check outdated deps:** `clojure -M:outdated`

## Purpose

Facade implements a fulcro-rad "skin" over multiple back end apis.

- **SWAPI**: The Star Wars API [https://swapi.dev](https://swapi.dev) - mostly working
- **Harry Potter**: The Harry Potter API [https://hp-api.onrender.com/](https://hp-api.onrender.com/) - TODO

## Core APIs

### 1. Starwars (SWAPI)

**Purpose**: Handle all operations related to the Star Wars API (SWAPI)

**Capabilities**:

- films
- people
- starships
- vehicles
- planets
- species

### 2. Harry Potter (HPAPI)

**Purpose**: Handle all operations related to the Harry Potter API (HWAPI)

**Capabilities**:

- characters

  - students
  - staff
  - house

- spells

## Configuration Management

Configuration is managed centrally with:

- Hierarchical configuration merging
- Hot-reload capabilities for configuration changes
- Validation of configuration parameters
- Fallback mechanisms for missing settings

### Development Guidelines

1. **Single Responsibility**: Each piece should have a clear, single responsibility
2. **Loose Coupling**: Pieces should minimize dependencies on each other
3. **Well-Defined Interfaces**: Use clear, consistent interfaces for piece communication
4. **Error Resilience**: Code should handle errors gracefully and provide meaningful feedback
5. **Performance Awareness**: Code should be designed with readability and performance both in mind

### Code Organization

- Code should be well-organized and modular
- Common functionality should be extracted to shared libraries
- Documentation should be comprehensive and up-to-date
- Configuration should be externalized

### Testing Guidelines

- All tests use **fulcro-spec** with **clojure.test**
- Test files use `.cljc` extension for cross-platform compatibility
- Use `deftest` for top-level test definitions
- Use `assertions` or `behavior` blocks for grouping related assertions
- Use `=>` for assertions instead of `is`
- For predicates, call them and assert true: `(string? x) => true` not `x => string?`
- `let` bindings must be outside `assertions` blocks
- Cannot use `=>` inside `doseq` - use regular `is` instead
- Common patterns:
  ```clojure
  (ns my-namespace-test
    (:require
     [clojure.test :refer [deftest is]]
     [fulcro-spec.core :refer [assertions =>]]))

  (deftest feature-test
    (assertions "does something specific"
      (+ 1 1) => 2
      (count [1 2 3]) => 3)
    
    (let [result {:name "test"}]
      (assertions "checks predicates"
        (map? result) => true
        (:name result) => "test")))
  ```
