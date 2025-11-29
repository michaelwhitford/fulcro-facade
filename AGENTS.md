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

All tests use **fulcro-spec** with **clojure.test** (not pure clojure.test or specification-based tests).

**Key Rules:**
- Test files use `.cljc` extension for cross-platform compatibility
- Use `deftest` for top-level test definitions (NOT `specification`)
- Use `assertions` blocks for grouping assertions within a deftest
- Use `=>` for assertions (preferred over `is` when possible)
- `let` bindings MUST be outside `assertions` blocks
- For predicates, call them and assert true: `(string? x) => true` NOT `x => string?`
- Cannot use `=>` inside `doseq` loops - use regular `is` instead
- Use reader conditionals `#?(:clj ...)` for platform-specific tests (e.g., file I/O)

**Common Patterns:**

```clojure
(ns my-namespace-test
  (:require
   [clojure.test :refer [deftest is]]
   [fulcro-spec.core :refer [assertions =>]]))

;; Basic assertions
(deftest simple-test
  (assertions "does something specific"
    (+ 1 1) => 2
    (count [1 2 3]) => 3))

;; Let bindings outside assertions
(deftest with-data-test
  (let [result {:name "test" :value 42}]
    (assertions "checks predicates and values"
      (map? result) => true           ;; predicate: call it, assert true
      (:name result) => "test"         ;; value: direct comparison
      (:value result) => 42)))

;; Doseq loops require regular is
(deftest loop-test
  (let [test-cases [["input1" "expected1"]
                    ["input2" "expected2"]]]
    (doseq [[input expected] test-cases]
      (is (= expected (my-fn input)) (str "Failed for: " input)))))

;; Platform-specific tests
#?(:clj
   (deftest clj-only-test
     (assertions "uses JVM-only features"
       (slurp (io/resource "file.txt")) => string?)))
```

**Common Mistakes to Avoid:**
- ❌ Using `specification` instead of `deftest`
- ❌ Using `behavior` as top-level (use in `deftest` only if needed)
- ❌ Putting `let` inside `assertions`: `(assertions (let [x 1] x => 1))`
- ❌ Using predicate as value: `result => map?` (should be `(map? result) => true`)
- ❌ Using `=>` inside `doseq` (use `is` instead)
