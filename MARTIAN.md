# MARTIAN - OpenAPI HTTP Clients

Martian provides **OpenAPI-driven HTTP clients**. Define a spec, get a typed client.

## Mental Model

```
OpenAPI Spec (YAML)
    ↓ bootstrap
Martian Client
    ↓ explore / response-for
API Calls with typed params
```

**Key insight**: You don't write HTTP calls. You explore operations and call them by name.

## Core Pattern

```clj
(require '[martian.core :as martian])

;; Discover what's available
(martian/explore client)
;; => [[:people "Return a list of people"] [:person "Returns a single person"] ...]

;; Inspect operation details
(martian/explore client :person)
;; => {:summary "..." :parameters {:id String} ...}

;; Execute
@(martian/response-for client :person {:id "1"})
;; => {:status 200 :body {:name "Luke Skywalker" ...}}
```

## Ad-Hoc API Exploration

Explore ANY API without full integration—create a temp spec:

```clj
;; 1. Create minimal spec
(spit "temp-api.yml" "
openapi: 3.0.0
info:
  title: JSONPlaceholder
  version: '1.0'
servers:
  - url: https://jsonplaceholder.typicode.com
paths:
  /posts/{id}:
    get:
      operationId: post
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      responses:
        '200':
          description: Post details
")

;; 2. Bootstrap client
(require '[martian.httpkit :as martian-http])
(def client (martian-http/bootstrap-openapi "temp-api.yml"))

;; 3. Explore and use
(martian/explore client)
@(martian/response-for client :post {:id 1})

;; 4. Cleanup
(clojure.java.io/delete-file "temp-api.yml")
```

## OpenAPI Spec Essentials

```yaml
paths:
  /items/{id}:
    get:
      operationId: get-item    # Becomes :get-item keyword
      parameters:
        - name: id
          in: path             # path = required
          required: true
        - name: search
          in: query            # query = optional
```

- `operationId` → keyword for `explore` and `response-for`
- `in: path` → required parameter
- `in: query` → optional parameter

## When to Fully Integrate

Ad-hoc exploration is great for testing. For long-term use:
- Add Mount component for lifecycle
- Add resolvers for EQL access
- Add RAD attributes for forms/reports

See **INTEGRATION_GUIDE.md** for the full process.

## See Also

- **AGENTS.md** — Project-specific client discovery
- **INTEGRATION_GUIDE.md** — Adding a new API end-to-end
