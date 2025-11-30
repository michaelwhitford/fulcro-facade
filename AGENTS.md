# AGENTS.md

Fulcro RAD application. Namespace: us.whitford.facade

Planning: PLAN.md | Changes: CHANGELOG.md | Scratchpad: SCRATCHPAD.md

## Using SCRATCHPAD.md

**SCRATCHPAD.md is your working scratchpad.** Use it to:

- Record your current task and approach
- Save useful REPL snippets and their outputs
- Track what you've tried and what worked
- Document discoveries about the system
- Note blockers or questions for the user

Update SCRATCHPAD.md as you work—it's your persistent memory across the session.

## REPL-Driven Development Philosophy

The REPL is how you think. Each evaluation builds understanding of the system.

**Approach:**
- Start small. One expression at a time. Build incrementally.
- Inspect constantly. Check the shape of data at each step.
- Work with real data. Pull values from the running system.
- Trust the REPL. It's not a scratchpad—it's your brain. The repo is your memory.

**Clojure principles:**
- One function, one job. If you can't explain it in one sentence, it should be more functions instead
- Data in, Data out.  Pure functions with side effects at the edges
- Plain data over abstractions.  Maps and Vectors compose, custom types hide
- Don't braid.  State, logic, IO should be separate
- Compose small pieces.  Combine with ->, ->>, comp, map instead of bigger functions

## REPL Setup (Do This First)

```clj
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))

;; Verify setup
(p {} [:account/count])
;; => #:account{:count 1}
```

## Self-Discovery via REPL

**Ask the REPL, not the docs, for "what exists" questions:**

| Question | Command |
|----------|---------|
| System status? | `(-> (p {} [:radar/overview]) :radar/overview :radar/summary)` |
| Quick counts? | `(->> (p {} [:radar/pathom-env]) :radar/pathom-env :counts)` |
| What APIs/clients exist? | `(->> (p {} [:radar/overview]) :radar/overview :radar/mount :states (filter #(re-find #"martian" %)))` |
| What queries can I make? | `(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :root (map :output))` |
| What fields does an entity have? | `(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :entity (filter #(re-find #"person" (:name %))) first :output)` |
| What computed/derived data? | `(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :derived (map (juxt :input :output)))` |
| What mutations can I call? | `(->> (p {} [:radar/pathom-env]) :radar/pathom-env :mutations (map :name))` |
| What forms/reports exist? | `(->> (p {} [:radar/overview]) :radar/overview :radar/reports (map (juxt :route :source :columns)))` |
| What forms with details? | `(->> (p {} [:radar/overview]) :radar/overview :radar/forms (map (juxt :name :route :id-key :read-only)))` |
| What are entity relationships? | `(-> (p {} [:radar/overview]) :radar/overview :radar/references)` |
| What operations does an API have? | `(require '[martian.core :as martian]) (require '[us.whitford.facade.components.swapi :refer [swapi-martian]]) (martian/explore swapi-martian)` |
| What component namespaces? | `(->> (p {} [:radar/overview]) :radar/overview :radar/mount :states (map #(second (re-find #"#'(.+)/" %))) set)` |

## Exploration Pattern

Build understanding incrementally:

```clj
;; 1. See what entry points exist
(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :root (map :output))

;; 2. Pick one, test minimal query
(p {} [{:swapi/all-people [:total]}])

;; 3. Expand based on resolver output
(p {} [{:swapi/all-people [:total {:results [:person/name]}]}])

;; 4. Follow relationships by ID
(p {} [{[:person/id "1"] [:person/name :person/homeworld]}])

;; 5. Chain derived resolvers (Pathom does this automatically!)
;; IP → city → weather in one query
(p {} [{[:ip-info/id "8.8.8.8"] [:ip-info/city :weather/temp-c :weather/description]}])
```

## Starter Examples

SWAPI and HPAPI are always available as reference implementations:

```clj
;; SWAPI - paginated (has :total/:results wrapper)
(p {} [{:swapi/all-people [:total {:results [:person/name :person/birth_year]}]}])
(p {} [{[:person/id "1"] [:person/name :person/height]}])

;; HPAPI - flat array (no wrapper)  
(p {} [{:hpapi/all-characters [:character/name :character/house]}])
(p {} [{:hpapi/all-spells [:spell/name :spell/description]}])

;; Explore API client operations
(martian/explore swapi-martian :person)
@(martian/response-for swapi-martian :person {:id "1"})
```

## Commands

| Task | Command |
|------|---------|
| Restart server | `(require 'development)(development/restart)` |
| Run tests | `clojure -M:run-tests` |
| Lint | `clj-kondo --lint .` |

## Agent ↔ User Communication

```clj
;; Notification (fire-and-forget) - CLJS REPL only
;; First: (shadow/repl :main) to connect to browser
(require '[us.whitford.facade.ui.toast :refer [toast!]])
(toast! "Task complete! 🤖")

;; Yes/No question (poll for answer) - CLJ REPL
(require '[us.whitford.facade.model.prompt :as prompt])
(def q (prompt/ask! "Deploy?"))
(prompt/get-result q)  ;; {:status :completed :answer true}
```

## Debugging

When queries fail or return empty:

```clj
;; Check if resolver exists for an attribute
(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :root
     (filter #(re-find #"account" (str (:output %)))))

;; Check entity resolver input requirements
(->> (p {} [:radar/pathom-env]) :radar/pathom-env :resolvers :entity
     (filter #(re-find #"account" (:name %)))
     first)
;; => {:name "account/id-resolver-datomic" :input [:account/id] :output [...]}

;; Debug UI state from CLJS REPL - see TROUBLESHOOTING.md 
;; "Debugging Fulcro App State from CLJS REPL" section

;; For more failure patterns, see TROUBLESHOOTING.md
```

## Documentation

**Use docs for "how/why" questions, not "what exists":**

| Need | Doc |
|------|-----|
| Fulcro concepts (normalization, idents, queries) | FULCRO.md |
| RAD concepts (attributes, forms, reports) | FULCRO-RAD.md |
| Pathom concepts (resolvers, input/output) | PATHOM.md |
| Statechart patterns (routing, workflows) | STATECHARTS.md |
| Martian library (OpenAPI clients) | MARTIAN.md |
| Radar library (runtime introspection) | RADAR.md |
| Adding a new API (step-by-step) | INTEGRATION_GUIDE.md |
| Common issues and solutions | TROUBLESHOOTING.md |
| Debug UI state issues | TROUBLESHOOTING.md (CLJS debug section) |
| Config, hot reload, agent comms | ARCHITECTURE.md |
