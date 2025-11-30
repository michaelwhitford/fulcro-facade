# Changelog

All notable changes to the Facade project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **RADAR.md**: Enhanced EQL query discovery documentation
  - Added `:counts` to pathom-env keys documentation
  - Added "API Output Shapes Vary" section with concrete examples for SWAPI, HPAPI, IPAPI
  - Added diagnostic tip for empty map results (wrong query shape)
  - Added forward-reference note linking to output shape examples

- **EQL.md**: Renamed from `eql-queries.txt` and expanded significantly
  - Added 20+ working copy-paste ready query examples
  - Organized by API pattern (SWAPI paginated, HPAPI flat, etc.)
  - Added quick discovery section with radar introspection patterns
  - Preserved EQL syntax reference with improved comments

- **ARCHITECTURE.md**: Added missing API integrations
  - Added IPAPI (IP Geolocation) section
  - Added Wttr (Weather) section
  - Updated API clients table with ipapi.clj and wttr.clj

- **AGENTS.md**: Condensed agent communication section
  - Simplified toast and prompt examples
  - Removed verbose workflow details (available in other docs)

### Added
- **Statechart-based prompt system** for AI agent interactions
  - New `model/prompt.cljc` with explicit state management (`:ask/idle`, `:ask/pending`, `:ask/completed`, `:ask/timeout`)
  - Built-in 60-second timeout support for question responses
  - Clean request/response correlation via session-id
  - Browser automatically polls for CLJ questions every 5 seconds
  - `prompt/ask!` for sending questions from CLJ REPL
  - `prompt/get-result` for polling question status and answers
  - `prompt/pending-questions` for viewing all pending questions
  - New `STATECHARTS.md` documentation
  - Legacy inbox approach preserved for backward compatibility

### Changed
- **Test suite improvements**: Added negative tests and removed placeholder test
  - Removed trivial `sample_spec.cljc` placeholder test (was just `1+1=2`)
  - Added negative tests for `str->int` (floats, whitespace, overflow)
  - Added negative tests for `ip->hex` (documents no input validation)
  - Added negative tests for `json->data` (empty structures, nested JSON)
  - Added negative tests for `swapiurl->id` (malformed URLs)
  - Added negative tests for `swapi-page->number` (non-numeric values)
  - Added negative tests for entity transformations (empty IDs, unknown namespaces)
  - Added negative tests for `parse-entity-id` (malformed IDs)
  - Removed unused require in `search_test.cljc`
  - Test count: 61 tests, 662 assertions (was 59 tests, 627 assertions)

- **Renamed `model/agent.cljc` to `model/prompt.cljc`** for clarity
  - `agent/ask!` → `prompt/ask!`
  - `agent/get-result` → `prompt/get-result`
  - `agent/pending-questions` → `prompt/pending-questions`
  - `@agent/agent-env` → `@prompt/prompt-env`
  - `@agent/pending-asks` → `@prompt/pending-prompts`
  - Session IDs now use `:prompt/` prefix instead of `:agent-ask/`
  - Updated all documentation (AGENTS.md, STATECHARTS.md, PLAN.md)

- **Documentation updates**:
  - AGENTS.md: Added statechart prompt approach as recommended method
  - README.md: Added STATECHARTS.md to documentation list
  - ARCHITECTURE.md: Updated component counts (15 mount states, 163 attributes, 12 forms, 11 reports, 16 entities, 15 references)

- **Code improvements**:
  - package.json: Fixed name from "fulcro-rad-tempate" typo to "facade"
  - ui/game.cljc: Added forward declaration for start-round! to resolve compilation warning
  - ui/toast.cljc: Refactored ask! to use statechart approach internally
  - client.cljs: Start prompt polling on app initialization
  - components/parser.clj: Register prompt resolvers in Pathom registry
  - development.clj: Import prompt-statecharts for REPL access

### Added
- **Landing page redesign** with hero header and feature cards
  - Hero section with robot icon and welcome message
  - Three feature cards highlighting APIs, Tech Stack, and Games
  - Info message showcasing AI-assisted development approach
  - MCP tooling requirements section with links to clojure-mcp and clojure-mcp-light repos

- **Tic-Tac-Toast game** - Play tic-tac-toe against an AI opponent
  - Grid-based toast UI for displaying the game board
  - Simple AI that prioritizes center and corner moves
  - Results sent to server via agent-comms channel
  - Menu button in top-right to start the game

- **AI agent interaction capabilities** for asking yes/no questions
  - `ask!` function in CLJS sends yes/no questions to user via toast notifications
  - User clicks Yes/No buttons, answer appears in CLJ inbox
  - Documented workflow in AGENTS.md for AI agents to poll for responses
  - Enables AI agents to get user confirmation before proceeding with tasks

- **Toast notification system** for AI agents
  - `toast!` function for sending notifications from CLJS REPL to browser
  - Customizable position and auto-close timing
  - Documented in AGENTS.md with examples

- **Whack-a-Toast! game** - Click toasts before they disappear
  - Menu button in top-right to start the game
  - 2 rounds: Round 1 (1.8s toasts), Round 2 (faster, 1s toasts)
  - Tracks score, misses, and reaction times
  - Results sent to server via agent-comms channel

- **Agent communication channel** (`model/agent_comms.cljc`)
  - Enables browser-to-REPL communication via Fulcro mutations
  - CLJS sends: `(comp/transact! @SPA [(agent/send-message {:message "hi" :data {}})])`
  - CLJ reads: `@us.whitford.facade.model.agent-comms/inbox`
  - Works with toast callbacks for async notification patterns
  - Useful for AI agents to receive feedback from the browser

- **Weather API integration** (wttr.in) with current conditions and 3-day forecast
  - OpenAPI spec (`wttr.yml`) defining weather forecast endpoint
  - Martian HTTP client component (`components/wttr.clj`)
  - Weather resolver with data transformations (`model/wttr.cljc`)
  - RAD attributes for weather and weather-day entities (`model_rad/wttr.cljc`)
  - Weather lookup widget with forecast cards (`ui/wttr_forms.cljc`)
  - "Weather" menu in navigation with "Get Forecast" option

- **"Use My Location" feature** for automatic weather by IP geolocation
  - Client-side IP detection via ipify.org
  - Pathom bridge resolver (`weather-from-ip-resolver`) connects IP → location → weather
  - Single query fetches IP info and weather data automatically:
    ```clojure
    [{[:ip-info/id "8.8.8.8"] [:ip-info/city :weather/temp-c :weather/description]}]
    ```

- **TROUBLESHOOTING.md**: New section on debugging Fulcro app state from CLJS REPL
  - How to inspect normalized state with `app/current-state`
  - Using `db->tree` to see denormalized props
  - Common issue: plain key vs join in component queries

### Fixed
- **Duplicate answer bug in ask!** - Using compare-and-set! prevents multiple answers from appearing in inbox
- **Tempids error in send-message mutation** - Fixed issue where mutation wasn't properly handling tempids
- IP Geolocation menu items now load components correctly
  - Added missing route registrations in `client.cljs` statechart configuration
  - Registered `IpLookupWidget`, `IpLookupList`, and `IpInfoForm` in `application-chart`

### Changed
- **AGENTS.md**: Enhanced with AI agent workflow documentation
  - Added step-by-step guide for AI agents to ask user questions
  - Documents toast notification system (`toast!` and `ask!`)
  - Clear examples for browser-to-REPL communication patterns
  
- Removed unused `MainRouter` from `root.cljc` (migrated to statechart routing)
- **INTEGRATION_GUIDE.md**: Added critical Step 10 for statechart route registration
  - Documents how to register routes in `client.cljs` `application-chart`
  - Explains when to use `ri/report-state`, `ri/form-state`, and `uir/rstate`
  - Added troubleshooting section for "menu click does nothing" issue
  - Updated checklist with ⚠️ warning for this commonly missed step
  - Added `client.cljs` to file location reference table

### Added
- Martian client exploration documentation in MARTIAN.md
  - REPL patterns for discovering API operations at runtime
  - `martian/explore` for listing operations and inspecting parameters
  - `martian/response-for` for executing requests
  - tap> debugging (user receives via shadow-cljs preload)

- Universal search feature combining SWAPI (Star Wars) and Harry Potter API results
  - New `entity.cljc` model with unified search resolver supporting 8 entity types
  - New `entity.cljc` RAD attributes for universal entity model
  - Support for both numeric IDs (SWAPI: `person-1`) and UUID IDs (HP: `character-{uuid}`)
  - Empty state handling - returns no results when search term is blank (prevents loading 570+ entities)
  - Test coverage with 59 tests and 627 assertions

- RADAR.md documentation for runtime introspection
  - EQL query discovery patterns
  - Resolver and entity diagnostics
  - Client state inspection guide

- ARCHITECTURE.md for high-level system overview

### Changed
- **AGENTS.md**: Simplified to focus on project-specific operations
  - Removed REPL setup details (delegated to tool layer)
  - Removed diagnostic details (moved to RADAR.md)
  - Added clear references to PLAN.md, CHANGELOG.md, and RADAR.md
  
- **PLAN.md**: Converted from comprehensive implementation plan to feature-focused documentation
  - Now documents Universal Search feature in detail
  - Includes data flow diagrams and implementation patterns
  - Documents control parameter flow and RAD report patterns
  
- **SearchReport**: Enhanced to support multiple entity types
  - Added Harry Potter characters and spells alongside SWAPI entities
  - Improved entity ID parsing to handle both numeric and UUID formats
  - Added entity-specific icons (magic, bolt, etc.)
  - Updated placeholder text to "Search Star Wars & Harry Potter..."

- **Search mutation**: Renamed `set-search-term` to `set-search-term-and-run`
  - Now triggers report execution automatically
  - Uses correct global control path for non-local controls
  - Added navigation to SearchReport route

- **Entity resolver**: Improved `all-entities-resolver`
  - Now accepts `:search-term` from query params
  - Parallel fetching from SWAPI and HP APIs
  - Better error handling with try/catch blocks

### Fixed
- SearchResultRow ident definition now uses keyword shorthand (`:ident :entity/id`)
- Entity ID parsing handles both SWAPI numeric IDs and HP UUID formats
- Control parameter storage uses correct global path `[::control/id ::search-term ::control/value]`

### Dependencies
- Updated deps.edn with latest Fulcro RAD and related libraries

## [0.1.0] - 2024-11-29

### Added
- Initial project setup with Fulcro RAD + Pathom3 + Datomic
- SWAPI integration for 6 entity types (People, Films, Planets, Species, Vehicles, Starships)
- Harry Potter API integration for Characters and Spells
- Statechart-based routing with 20 configured routes
- Server-side pagination for SWAPI reports
- File upload support with blob storage
- Account management with Datomic persistence
- Test suite with fulcro-spec (53 initial tests)
- Development environment with hot reload
