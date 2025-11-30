# MARTIAN.md

Martian HTTP client exploration patterns for runtime API discovery.

## Overview

Martian provides OpenAPI-driven HTTP clients. Use these REPL patterns to discover available operations, inspect parameters, and execute requests.

**Two modes of use:**
1. **Existing clients** - Explore pre-configured APIs (SWAPI, HPAPI, etc.)
2. **Ad-hoc exploration** - Create a temporary spec to explore ANY API

## Quick Start

```clj
(require '[martian.core :as martian])
(require '[us.whitford.facade.components.swapi :refer [swapi-martian]])

;; List all operations
(martian/explore swapi-martian)
;; => [[:people "Get all people"] [:person "Get person by ID"] ...]

;; Inspect specific operation
(martian/explore swapi-martian :person)
;; => {:summary "Get person by ID"
;;     :parameters [{:name :id :in :path :required true}]
;;     :returns {...}}

;; Execute request
@(martian/response-for swapi-martian :person {:id "1"})
;; => {:status 200 :body {:name "Luke Skywalker" ...} :opts {...} :headers {...}}
```

## Available Clients

| Client | Var | Operations |
|--------|-----|------------|
| SWAPI | `swapi-martian` | 12 operations |
| HPAPI | `hpapi-martian` | 2 operations |
| IP API | `ipapi-martian` | 1 operation |
| Weather | `wttr-martian` | 1 operation |

## SWAPI Operations

```clj
(require '[us.whitford.facade.components.swapi :refer [swapi-martian]])
(martian/explore swapi-martian)
```

| Operation | Description | Parameters |
|-----------|-------------|------------|
| `:people` | Get all people | `:search`, `:page` |
| `:person` | Get person by ID | `:id` |
| `:films` | Get all films | `:search`, `:page` |
| `:film` | Get film by ID | `:id` |
| `:planets` | Get all planets | `:search`, `:page` |
| `:planet` | Get planet by ID | `:id` |
| `:species` | Get all species | `:search`, `:page` |
| `:specie` | Get species by ID | `:id` |
| `:vehicles` | Get all vehicles | `:search`, `:page` |
| `:vehicle` | Get vehicle by ID | `:id` |
| `:starships` | Get all starships | `:search`, `:page` |
| `:starship` | Get starship by ID | `:id` |

## HPAPI Operations

```clj
(require '[us.whitford.facade.components.hpapi :refer [hpapi-martian]])
(martian/explore hpapi-martian)
```

| Operation | Description | Parameters |
|-----------|-------------|------------|
| `:characters` | Get all characters | none |
| `:spells` | Get all spells | none |

## IP API Operations

```clj
(require '[us.whitford.facade.components.ipapi :refer [ipapi-martian]])
(martian/explore ipapi-martian)
```

| Operation | Description | Parameters |
|-----------|-------------|------------|
| `:ip-lookup` | Get geolocation for IP | `:ip` |

## Weather API Operations

```clj
(require '[us.whitford.facade.components.wttr :refer [wttr-martian]])
(martian/explore wttr-martian)
```

| Operation | Description | Parameters |
|-----------|-------------|------------|
| `:forecast` | Get weather forecast | `:location`, `:format` |

## Debugging with tap>

All martian clients include tap> interceptors for debugging:

```clj
;; Requests and responses are automatically tapped
;; View in Portal, REBL, or shadow-cljs console

@(martian/response-for swapi-martian :person {:id "1"})
;; tap> receives: {:type :martian/request :url "..." :method :get ...}
;; tap> receives: {:type :martian/response :status 200 :body {...} ...}
```

## Common Patterns

### Explore Before Querying

```clj
;; Always explore first to understand available params
(martian/explore swapi-martian :people)
;; => Shows :search and :page are optional params

;; Then query with confidence
@(martian/response-for swapi-martian :people {:search "skywalker" :page 1})
```

### Handle Errors

```clj
(let [{:keys [status body]} @(martian/response-for swapi-martian :person {:id "999"})]
  (case status
    200 (println "Found:" (:name body))
    404 (println "Not found")
    (println "Error:" status)))
```

### Paginated Results

```clj
;; SWAPI returns pagination info
(let [{:keys [body]} @(martian/response-for swapi-martian :people {:page 1})]
  {:count (:count body)
   :next (:next body)
   :results (count (:results body))})
;; => {:count 82 :next "https://swapi.dev/api/people/?page=2" :results 10}
```

## Adding Custom Interceptors

See `components/interceptors.clj` for examples:

```clj
(def my-interceptor
  {:name ::my-interceptor
   :enter (fn [ctx]
            (println "Request:" (get-in ctx [:request :url]))
            ctx)
   :leave (fn [ctx]
            (println "Response:" (get-in ctx [:response :status]))
            ctx)})
```

---

## Ad-Hoc API Exploration

Explore ANY API by creating a temporary OpenAPI spec. No component setup required.

### Step 1: Create Minimal Spec

```clj
(spit "temp-api.yml" "
openapi: 3.0.0
info:
  title: JSONPlaceholder
  version: '1.0'
servers:
  - url: https://jsonplaceholder.typicode.com
paths:
  /posts:
    get:
      operationId: posts
      summary: Get all posts
      responses:
        '200':
          description: List of posts
  /posts/{id}:
    get:
      operationId: post
      summary: Get post by ID
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      responses:
        '200':
          description: Post details
  /users:
    get:
      operationId: users
      summary: Get all users
      responses:
        '200':
          description: List of users
")
```

### Step 2: Bootstrap & Explore

```clj
(require '[martian.httpkit :as martian-http])
(require '[martian.core :as martian])

;; Create client directly from spec (no Mount state needed)
(def client (martian-http/bootstrap-openapi "temp-api.yml" 
              {:server-url "https://jsonplaceholder.typicode.com"}))

;; Explore available operations
(martian/explore client)
;; => [[:posts "Get all posts"] [:post "Get post by ID"] [:users "Get all users"]]

;; Inspect specific operation
(martian/explore client :post)
;; => {:summary "Get post by ID", :parameters {:id Int}, :returns {200 nil}}

;; Execute request
@(martian/response-for client :post {:id 1})
;; => {:status 200, :body {:userId 1, :id 1, :title "...", :body "..."}}
```

### Step 3: Cleanup

```clj
(clojure.java.io/delete-file "temp-api.yml")
```

## OpenAPI Spec Template

Minimal template for creating specs:

```yaml
openapi: 3.0.0
info:
  title: API Name
  version: '1.0'
servers:
  - url: https://api.example.com
paths:
  /items:
    get:
      operationId: list-items      # <- Becomes :list-items keyword
      summary: List all items
      parameters:                  # Optional query params
        - name: page
          in: query
          schema:
            type: integer
        - name: search
          in: query
          schema:
            type: string
      responses:
        '200':
          description: Success
  /items/{id}:
    get:
      operationId: get-item        # <- Becomes :get-item keyword
      summary: Get item by ID
      parameters:
        - name: id
          in: path                 # Path params are required
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Item details
        '404':
          description: Not found
```

**Key points:**
- `operationId` becomes the keyword for `explore` and `response-for`
- `in: path` parameters are required
- `in: query` parameters are optional
- Spec file goes in `src/main/` directory (or use absolute path)

## AI Workflow: Discovering a New API

1. **Research** - Find API docs, note base URL and key endpoints
2. **Create spec** - Write minimal YAML with 1-2 endpoints
3. **Bootstrap** - `martian-http/bootstrap-openapi` (no integration needed)
4. **Explore** - Use `martian/explore` to see operations and params
5. **Test** - Execute requests with `martian/response-for`
6. **Iterate** - Add more endpoints to spec as needed
7. **Decide** - If useful long-term, follow INTEGRATION_GUIDE.md
8. **Cleanup** - Delete temp spec if not integrating

> **Ready to fully integrate an API?**  
> Follow [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) for the complete 5-layer 
> integration: OpenAPI → Component → Model → RAD → UI

## See Also

- `INTEGRATION_GUIDE.md` - Full API integration (5 layers)
- `AGENTS.md` - Quick REPL examples
- `components/*.clj` - Existing client implementations
- `src/main/*.yml` - OpenAPI spec examples (ipapi.yml, swapi.yml, hpapi.yml)
