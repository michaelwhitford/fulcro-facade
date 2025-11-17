# AGENTS.md

## Overview

Facade provides a client for multiple backend apis using fulcro and fulcro-rad.

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

## Running tests

You can run tests from the repository base directory in bash like this:

```
clojure -M:dev:test:cljs:run-tests
```

### Unit Testing

### Integration Testing

### Performance Testing

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
