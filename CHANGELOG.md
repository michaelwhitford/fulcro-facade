# Changelog

All notable changes to the Facade project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
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
- IP Geolocation menu items now load components correctly
  - Added missing route registrations in `client.cljs` statechart configuration
  - Registered `IpLookupWidget`, `IpLookupList`, and `IpInfoForm` in `application-chart`

### Changed
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
