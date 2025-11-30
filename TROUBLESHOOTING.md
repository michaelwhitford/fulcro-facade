# Troubleshooting Guide

Common issues and solutions when developing with Facade.

## Server Issues

### Server Won't Start

**Symptoms**: Error on `(development/restart)` or server startup

**Common Causes**:
1. **Port already in use**
   - Solution: `lsof -i :3010` and kill the process, or change port in `config/defaults.edn`

2. **Circular dependency**
   - Check for circular requires between namespaces
   - Solution: Move shared code to `components/utils.cljc`

3. **Missing config key**
   - Error: "No value found for key :some/config"
   - Solution: Add the key to `config/defaults.edn`

4. **Syntax error in CLJ file**
   - Run `clj-kondo --lint .` to find syntax errors
   - Check the error message for filename and line number

### Server Restarts But Code Changes Don't Apply

**Solution**: 
```clojure
;; Hard restart
(mount/stop)
(mount/start)

;; Or use development namespace
(development/restart)
```

If still not working:
1. Check if the namespace is being reloaded (watch console output)
2. Ensure you're editing the right file (not a copy)
3. Try stopping/starting REPL entirely

## Resolver Issues

### "No resolver found for <attribute>"

**Symptoms**: Parser returns empty map or error about unreachable attribute

**Diagnosis**:
```clojure
;; Check if resolver is registered
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))
(p {} [:radar/pathom-env])
;; Look for your resolver in the output
```

**Solutions**:
1. **Resolver not exported**
   - Check that resolver is in the `resolvers` var in `model/<api>.cljc`
   ```clojure
   (def resolvers [thing-resolver all-things-resolver])  ; Must export!
   ```

2. **Resolver not registered in parser**
   - Add to `all-resolvers` in `components/parser.clj`
   ```clojure
   (def all-resolvers
     [m.account/resolvers
      m.myapi/resolvers])  ; ADD THIS
   ```

3. **Wrong output declaration**
   - Check `::pco/output` matches your query
   ```clojure
   ;; Query: [:thing/name]
   ;; Output must include:
   {::pco/output [:thing/name]}
   ```

4. **Identity not connected**
   - For entity resolvers, input must match identity
   ```clojure
   ;; Query: [:thing/id "1"]
   ;; Resolver params must destructure:
   [env {:thing/keys [id]}]
   ```

### Resolver Runs But Returns Empty

**Diagnosis**: Add `tap>` to see what's happening:
```clojure
(pco/defresolver thing-resolver [env {:thing/keys [id] :as params}]
  {::pco/output [:thing/id :thing/name]}
  (tap> {:from ::thing-resolver :id id :params params})
  (let [result (fetch-thing id)]
    (tap> {:result result})
    result))
```

You can also test resolvers directly:
```clojure
(require '[us.whitford.fulcro-radar.api :as radar])
(def p (radar/get-parser))
(p {} [{[:thing/id "1"] [:thing/name]}])
```

**Common Causes**:
1. **Exception being caught and returning `{}`**
   - Check logs for error messages
   - Temporarily remove try-catch to see the actual error

2. **API returning unexpected format**
   - Use `tap>` to inspect raw API response
   - Check if keys need to be transformed

3. **nil being returned**
   - Pathom treats `nil` as "no data"
   - Return `{}` instead for "empty but successful"

## UI/Frontend Issues

### Debugging Fulcro App State from CLJS REPL

When UI components show blank data despite backend queries working, the issue is usually 
with normalization/denormalization. Here's how to debug:

**1. Connect to CLJS REPL**
```clojure
;; From shadow-cljs REPL, elevate to browser context
(shadow/repl :main)
```

**2. Inspect Raw App State**
```clojure
(require '[com.fulcrologic.fulcro.application :as app])
(require '[us.whitford.facade.application :refer [SPA]])

;; Get current state atom
(def state (app/current-state @SPA))

;; Check component's state in the normalized db
(get-in state [:component/id :my.ns/MyComponent])
;; => {:some-key "value", :result [:thing/id "123"]}
;;                                 ^^^^^^^^^^^^^^^ This is an ident!
```

**3. Check Data at Ident**

If you see an ident like `[:thing/id "123"]`, the data is normalized. Check what's stored there:
```clojure
(get-in state [:thing/id "123"])
;; => {:thing/id "123" :thing/name "Widget" :thing/items [[:item/id "a"] [:item/id "b"]]}
```

**4. Simulate What Fulcro Passes to Component**

Use `db->tree` to see the denormalized data the component actually receives as props:
```clojure
(require '[com.fulcrologic.fulcro.algorithms.denormalize :as fdn])
(require '[com.fulcrologic.fulcro.components :as comp])
(require '[my.ns :as my])

;; Get component's query
(def query (comp/get-query my/MyComponent))

;; Get component's ident
(def ident [:component/id :my.ns/MyComponent])

;; Denormalize - this is exactly what Fulcro passes as props!
(fdn/db->tree query (get-in state ident) state)
;; => {:some-key "value", 
;;     :result {:thing/id "123" 
;;              :thing/name "Widget" 
;;              :thing/items [{:item/id "a" :item/name "Item A"} ...]}}
```

**5. Common Issue: Plain Key vs Join**

If `:result` shows an ident but the component expects data, the query is wrong:

❌ **Wrong** - plain key doesn't denormalize:
```clojure
{:query [:location :result]}  ; :result stays as ident
```

✅ **Correct** - join denormalizes the data:
```clojure
{:query [:location {:result (comp/get-query ResultComponent)}]}
```

**6. Verify Component Query Structure**
```clojure
(comp/get-query my/MyComponent)
;; Should show joins for nested data:
;; [:location {:result [:thing/id :thing/name {:thing/items [...]}]}]
```

### Form Not Loading Data

**Symptoms**: Form displays but fields are empty

**Diagnosis**:
```clojure
;; Check if query is correct
(require '[com.fulcrologic.fulcro.components :as comp])
(comp/get-query MyForm)

;; Test query in REPL (using p from earlier setup)
(p {} [{[:thing/id "1"] (comp/get-query MyForm)}])
```

**Solutions**:
1. **Query doesn't match resolver output**
   - Form query: `[:thing/id :thing/name]`
   - Resolver output must include both

2. **Missing identity**
   - Form loads by identity (e.g., `[:thing/id "1"]`)
   - Check that entity has that ID in the data

3. **Route segment not defined**
   - Forms need route segment for statechart routing
   ```clojure
   (form/defsc-form MyForm [this props]
     {fo/route-prefix "my-form"  ; ADD THIS
      ;; ...
      })
   ```

### Report Shows No Data

**Symptoms**: Report component renders but table is empty

**Solutions**:
1. **Collection resolver not returning right format**
   ```clojure
   ;; Must return this shape:
   {:myapi/all-things {:results [...]
                       :total 100}}
   ```

2. **source-attribute doesn't match resolver**
   ```clojure
   (report/defsc-report MyReport [this props]
     {ro/source-attribute :myapi/all-things  ; Must match resolver output key
      ;; ...
      })
   ```

3. **Column attributes not in results**
   - Columns must be in resolver's `::pco/output`
   - Check that query includes all column attributes

### Navigation Not Working

**Symptoms**: Clicking menu items doesn't change route

**Solutions**:
1. **Component not in router targets**
   ```clojure
   (defrouter MainRouter [this props]
     {:router-targets [LandingPage MyForm MyReport]  ; ADD HERE
      ;; ...
      })
   ```

2. **Missing route-segment**
   - Components need `:route-segment` for routing
   ```clojure
   (defsc MyPage [this props]
     {:route-segment ["my-page"]  ; ADD THIS
      ;; ...
      })
   ```

3. **Using wrong routing function**
   - Use `uir/route-to!` with statechart routing (not `rroute/route-to!`)

## Martian/API Issues

### "No available operations" from Martian

**Symptoms**: `(martian/explore client)` returns empty vector

**Solutions**:
1. **OpenAPI spec not found**
   - Check file path in config
   - Path is relative to `src/main/`: `"myapi.yml"` → `src/main/myapi.yml`

2. **OpenAPI spec has errors**
   - Validate spec: https://editor.swagger.io
   - Check that `operationId` is defined for each endpoint

3. **Component not started**
   - Check Mount state: `(mount/running-states)`
   - Restart: `(development/restart)`

### API Returns 404 or Error

**Diagnosis**:
```clojure
;; Check what URL Martian is hitting
@(martian/response-for myapi-martian :operation {:param "value"})
;; Look at :opts :url in response
```

**Solutions**:
1. **Wrong server URL in config**
   - Check `config/defaults.edn`
   - Verify against API documentation

2. **Missing path parameter**
   - Check operation definition: `(martian/explore client :operation)`
   - Ensure all required params are provided

3. **API rate limiting**
   - Add delays between requests
   - Check API documentation for rate limits

### Martian Returns Unparsed String

**Symptoms**: Response body is a string instead of map

**Solution**: API might not be returning `application/json`
```clojure
;; Check response content-type
(let [resp @(martian/response-for client :op params)]
  (tap> {:headers (:headers resp)}))

;; If needed, parse manually
(require '[clojure.data.json :as json])
(json/read-str body :key-fn keyword)
```

## Attribute Issues

### "Attribute <key> not found"

**Symptoms**: Error when trying to use attribute in form/report

**Solutions**:
1. **Attribute not registered**
   - Add to attributes vector in `model_rad/<api>.cljc`
   ```clojure
   (def attributes [thing_id thing_name])  ; ADD HERE
   ```

2. **Attributes not exported to global registry**
   - Add to `model_rad/attributes.cljc`
   ```clojure
   (def all-attributes
     (concat
       ;; ...
       m.myapi/attributes))  ; ADD THIS
   ```

3. **Typo in attribute keyword**
   - Check that `:thing/name` matches attribute definition
   - Use namespace completion in editor to avoid typos

### Wrong Attribute Type

**Symptoms**: Form field renders incorrectly (e.g., dropdown for string)

**Solution**: Check attribute type definition:
```clojure
(defattr thing_name :thing/name :string  ; Not :ref!
  {ao/identities #{:thing/id}})
```

Common types:
- `:string` - Text
- `:int` - Integer
- `:double` - Float
- `:boolean` - True/false
- `:ref` - Reference to another entity
- `:instant` - Date/time

## Build/Compilation Issues

### CLJS Won't Compile

**Symptoms**: Shadow-cljs shows compilation errors

**Solutions**:
1. **Namespace doesn't exist**
   - Check require statements
   - Ensure file is in correct location

2. **Macro used wrong**
   - Check if you need reader conditional: `#?(:cljs ...)`
   - Some macros are CLJ-only

3. **Symbol conflicts**
   - Check for `:refer :all` that might import conflicting names
   - Use `:as` aliases instead

### Linter Errors

**Command**: `clj-kondo --lint .`

**Expected output**: 2 errors, 2 warnings, 1 info

These are intentional warts left to verify AI agents check lint output correctly:
- `lib/logging.clj:46-47` - Unresolved `_`, `err` - false positive from taoensso.encore/if-let
- `model/hpapi.cljc` - Unused UUID import
- `model/swapi.cljc` - Unused UUID import
- `.clj-kondo/imports/.../clj_kondo_hooks.clj` - Info about single arg to `str` (guardrails import)

**Common fixable errors**:
1. **Unresolved symbol**
   - Missing require
   - Typo in name

2. **Unused import**
   - Remove the require
   - Or use it

3. **Invalid arity**
   - Wrong number of arguments to function
   - Check function signature

## Database Issues

### Datomic Connection Failed

**Symptoms**: Error on server startup about Datomic

**Solution**: Check config in `config/defaults.edn`:
```clojure
:com.fulcrologic.rad.database-adapters.datomic/databases
{:main {:datomic/schema   :production
        :datomic/client   {:server-type :dev-local  ; or :peer-server, :cloud
                           :storage-dir :mem        ; or filesystem path
                           :system      "ci"}
        :datomic/database "example"}}
```

For development, `:storage-dir :mem` uses in-memory database (no persistence).

### Schema Migration Failed

**Symptoms**: Error about missing schema or attributes

**Solution**: 
1. Ensure attributes have `:ao/schema :production`
2. Restart server to re-generate schema
3. Check logs for schema migration errors

## Configuration Issues

### Config Key Not Found

**Symptoms**: `No value found for key :some/namespace/config`

**Solution**: Add to `config/defaults.edn`:
```clojure
:us.whitford.facade.components.myapi/config {:swagger-file "myapi.yml"
                                             :server-url "https://api.example.com"}
```

**Important**: Key must be namespaced with component namespace!

### Config Changes Not Applied

**Solution**: Restart server - config is only loaded at startup:
```clojure
(development/restart)
```

## Performance Issues

### Slow API Responses

**Solutions**:
1. **Add caching**
   ```clojure
   (def cache (atom {}))
   
   (defn cached-fetch [id]
     (or (get @cache id)
         (let [result (api-fetch id)]
           (swap! cache assoc id result)
           result)))
   ```

2. **Batch requests**
   - Use Pathom's batch resolvers
   - Combine multiple API calls

3. **Reduce data transfer**
   - Use API field filtering
   - Only request needed fields

### Slow Report Loading

**Solutions**:
1. **Use pagination**
   ```clojure
   (report/defsc-report MyReport [this props]
     {ro/machine spr/machine
      ro/page-size 10  ; ADD THIS
      ;; ...
      })
   ```

2. **Limit columns**
   - Only show essential columns
   - Use detail form for other fields

3. **Add search/filtering**
   - Reduce data fetched from API
   - Filter server-side when possible

## REPL Issues

### REPL Won't Connect

**Solutions**:
1. **Check nREPL is running**
   - Look for "nREPL server started" in startup output
   - Default port: Check `deps.edn` or logs

2. **Wrong port**
   - Check `.nrepl-port` file in project root
   - Connect to that port

3. **Restart REPL entirely**
   - Stop and restart the JVM process

### REPL Hangs

**Solutions**:
1. **Infinite loop**
   - Press Ctrl+C to interrupt
   - Check code for infinite loops/recursion

2. **Blocking operation**
   - API call taking too long
   - Use `@(future ...)` for async

3. **Out of memory**
   - Restart REPL
   - Increase JVM heap size in `deps.edn`

## Getting Help

### Debug Checklist

When something isn't working:

1. **Check the logs**
   - Server console shows all log output
   - Look for ERROR or WARN messages

2. **Test each layer independently**
   ```clojure
   ;; 1. Test Martian client
   @(martian/response-for client :op params)
   
   ;; 2. Test model function
   (m.myapi/fetch-thing "1")
   
   ;; 3. Test resolver
   (p {} [{[:thing/id "1"] [:thing/name]}])
   
   ;; 4. Test in UI
   ```

3. **Use tap>**
   ```clojure
   (tap> {:debug "value"})
   ;; View in Portal or REPL
   ```

4. **Check runtime state**
   ```clojure
   (require '[us.whitford.fulcro-radar.api :as radar])
   (def p (radar/get-parser))
   
   ;; See all resolvers
   (p {} [:radar/pathom-env])
   
   ;; See app state (from CLJS repl)
   @(::app/state-atom @SPA)
   ```

5. **Simplify**
   - Remove complexity until it works
   - Add back piece by piece

### Ask For Help

Include in your question:
1. **What you're trying to do**
2. **What you expected**
3. **What actually happened** (error message, behavior)
4. **What you've tried**
5. **Relevant code snippets**
6. **Logs/errors**

### Useful Docs

- **INTEGRATION_GUIDE.md** - Adding new APIs
- **QUICK_REFERENCE.md** - Common patterns
- **RADAR.md** - Runtime introspection
- **MARTIAN.md** - API client debugging

### Community Resources

- [Fulcro Docs](https://book.fulcrologic.com)
- [RAD Docs](https://github.com/fulcrologic/fulcro-rad)
- [Pathom Docs](https://pathom3.wsscode.com)
- [Clojurians Slack](http://clojurians.net) - #fulcro channel
