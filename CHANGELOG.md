# Changelog

All notable changes to the Facade project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
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
