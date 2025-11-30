# Fulcro API Client: facade

A Fulcro RAD application that serves as a client to multiple API backends. Built as a learning project to explore Fulcro and Fulcro RAD patterns.

## Features

- **Universal Search** - Search across Star Wars and Harry Potter universes from a single interface
- **SWAPI Integration** - People, Films, Planets, Species, Vehicles, Starships ([swapi.dev](https://swapi.dev))
- **Harry Potter API** - Characters and Spells ([hp-api.onrender.com](https://hp-api.onrender.com))
- **10 Forms, 10 Reports** - Full RAD-based CRUD with statechart routing
- **Account Management** - Datomic-backed user accounts with file uploads
- **Test Suite** - 59 tests with 627 assertions

## Documentation

| File | Description |
|------|-------------|
| `AGENTS.md` | Quick reference for AI assistants and developers |
| `RADAR.md` | Runtime introspection and EQL query discovery |
| `ARCHITECTURE.md` | System overview and component structure |
| `PLAN.md` | Feature documentation and implementation details |
| `CHANGELOG.md` | Version history |

## Usage

```bash
# Run tests
clojure -M:run-tests

# Lint
clj-kondo --lint .

# Check for outdated dependencies
clojure -M:outdated
```

## Building the SPA

Compile the CLJS source to run the client:

```bash
yarn                      # or: npm install
shadow-cljs watch main    # development with hot reload
```

For production builds:

```bash
make release
```

## Running the Server

The server uses an in-memory Datomic database. Start from the REPL with the `:dev` alias:

```bash
clj -A:dev
```

```clojure
user=> (require 'development)
user=> (development/start)
```

## VSCode + Calva

Add this to `.vscode/settings.json` or your Calva connect sequences:

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

## Development Functions

| Function | Description |
|----------|-------------|
| `(development/start)` | Cold start the server |
| `(development/stop)` | Stop the server |
| `(development/restart)` | Stop, refresh all source, and restart |
| `(development/fast-restart)` | Stop and restart without refreshing |

> **Tip:** Use `restart` when you've modified middleware or closed-over functions. For simple changes, evaluate directly in the REPL.

## License

The MIT License (MIT)  
Copyright (c), Michael Whitford

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
