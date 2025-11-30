# facade

A self-building Fulcro RAD application designed to be extended by AI agents.

## What Is This?

This project is a **bootstrapped foundation** for AI-assisted application development. It provides:

- **AI-ready documentation** - `AGENTS.md` gives AI assistants everything they need to understand and modify the codebase
- **Integration patterns** - Clear examples showing how to add new API backends
- **Layered architecture** - Separation of concerns that AI agents can reason about and extend
- **Working examples** - Real integrations to learn from, not just documentation

The goal: point an AI agent at this repo and say "add support for X API" — and it works.

## AI Agent Requirements

To work with this project, your AI agent needs REPL access via one of these tools:

| Tool                                                              | Description                                                         |
| ----------------------------------------------------------------- | ------------------------------------------------------------------- |
| [clojure-mcp](https://github.com/bhauman/clojure-mcp)             | Full-featured MCP server with REPL, file editing, and more          |
| [clojure-mcp-light](https://github.com/bhauman/clojure-mcp-light) | Lightweight `clj-nrepl-eval` and `clj-paren-repair` tools for NREPL |

Either tool provides the essential capability: **live REPL evaluation**. This lets the AI agent test code incrementally, inspect runtime state, and verify changes work before committing.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                           │
│              (Fulcro RAD Forms & Reports)               │
├─────────────────────────────────────────────────────────┤
│                 Statechart Routing                      │
│              (Route registration in client.cljs)        │
├─────────────────────────────────────────────────────────┤
│                    RAD Attributes                       │
│              (Schema + UI hints in one place)           │
├─────────────────────────────────────────────────────────┤
│                  Pathom Resolvers                       │
│              (Data fetching + transformation)           │
├─────────────────────────────────────────────────────────┤
│                   Martian Clients                       │
│              (OpenAPI-driven HTTP clients)              │
├─────────────────────────────────────────────────────────┤
│                   OpenAPI Specs                         │
│              (Contract for external APIs)               │
└─────────────────────────────────────────────────────────┘
```

Adding a new API integration requires touching these files:

| Step | File | Purpose |
|------|------|---------|
| 1 | `src/main/<api>.yml` | OpenAPI spec |
| 2 | `components/<api>.clj` | Martian HTTP client |
| 3 | `config/defaults.edn` | API configuration |
| 4 | `model/<api>.cljc` | Resolvers + business logic |
| 5 | `model_rad/<api>.cljc` | RAD attribute definitions |
| 6 | `model_rad/attributes.cljc` | Register attributes |
| 7 | `components/parser.clj` | Register resolvers |
| 8 | `ui/<api>_forms.cljc` | Forms and reports |
| 9 | `ui/root.cljc` | Menu items |
| 10 | `client.cljs` | **Statechart route registration** ⚠️ |

Step 10 is critical — without registering routes in the statechart, menu clicks won't load components.

## Example Integrations

The repo includes working integrations of varying complexity:

| Integration | Complexity | Demonstrates |
|-------------|------------|--------------|
| IP Geolocation (`ipapi`) | Simple | Single entity, no relationships |
| Weather (`wttr`) | Simple | Dependent resolver (uses IP location) |
| Harry Potter API (`hpapi`) | Medium | Multiple entities, filtering |
| SWAPI (`swapi`) | Complex | Relationships, pagination |

These serve as templates. See `INTEGRATION_GUIDE.md` for the step-by-step process.

## Documentation for AI Agents

| File                   | Purpose                                             |
| ---------------------- | --------------------------------------------------- |
| `AGENTS.md`            | **Start here** — Commands, patterns, file locations |
| `INTEGRATION_GUIDE.md` | Step-by-step for adding new APIs                    |
| `ARCHITECTURE.md`      | System overview and data flow                       |
| `QUICK_REFERENCE.md`   | Common patterns and pitfalls                        |
| `TROUBLESHOOTING.md`   | Error diagnosis and fixes                           |

Supporting concept guides: `FULCRO.md`, `FULCRO-RAD.md`, `PATHOM.md`, `MARTIAN.md`, `RADAR.md`

## Quick Start

```bash
# Install dependencies
yarn

# Start shadow-cljs (terminal 1)
shadow-cljs watch main

# Start REPL (terminal 2)
clj -A:dev
```

```clojure
(require 'development)
(development/start)
```

Open http://localhost:3000

### VSCode + Calva

Add to `.vscode/settings.json` or Calva connect sequences:

```json
{
  "name": "fulcro-rad",
  "projectType": "deps.edn",
  "cljsType": "shadow-cljs",
  "afterCLJReplJackInCode": "(require 'development :reload) (in-ns 'development) (start)",
  "menuSelections": {
    "cljAliases": ["dev", "cljs", "test"],
    "cljsLaunchBuilds": [":main"],
    "cljsDefaultBuild": ":main"
  }
}
```

## Development

```bash
clojure -M:run-tests      # Run tests
clj-kondo --lint .        # Lint
clojure -M:outdated       # Check dependencies
```

| Function                | Description                           |
| ----------------------- | ------------------------------------- |
| `(development/start)`   | Cold start the server                 |
| `(development/stop)`    | Stop the server                       |
| `(development/restart)` | Stop, refresh all source, and restart |

## License

MIT License — Copyright (c) Michael Whitford
