# PLAY.md - Martian Utility Evaluation Report

**Date:** 2025-11-30  
**Purpose:** Evaluate MARTIAN.md documentation effectiveness for AI-guided API exploration

---

## Executive Summary

The MARTIAN.md documentation is **excellent for AI guidance**. It provides clear, actionable patterns that an AI can follow to discover and execute API operations at runtime. All documented features work as described.

### Verification Status

| Feature | Status | Notes |
|---------|--------|-------|
| `martian/explore` (list) | ✅ Works | Returns vector of `[operation-keyword description]` |
| `martian/explore` (detail) | ✅ Works | Returns `{:summary :parameters :returns}` |
| `martian/response-for` | ✅ Works | Returns promise, deref to get result |
| `martian/request-for` | ✅ Works | Preview request without executing |
| `martian/url-for` | ✅ Works | Get URL only |
| Optional/Required params | ✅ Works | `OptionalKey` wrapper distinguishes them |
| tap> interceptors | ✅ Works | Requests/responses logged automatically |
| SWAPI pagination | ✅ Works | `:page` param, body has `:count :next :previous :results` |
| HPAPI direct arrays | ✅ Works | No pagination wrapper |

---

## Test Results

### 1. Operation Discovery

**SWAPI Operations:**
```clojure
(martian/explore swapi-martian)
;; => [[:starship "Returns a single starship"]
;;     [:specie "Returns a single specie"]
;;     [:vehicles "Returns a list of vehicles"]
;;     [:person "Returns a single person"]
;;     [:films "Returns a list of films"]
;;     [:starships "Returns a list of starships"]
;;     [:vehicle "Returns a single vehicle"]
;;     [:species "Returns a list of species"]
;;     [:film "Returns a single film"]
;;     [:planets "Returns a list of planets"]
;;     [:people "Return a list of people"]
;;     [:planet "Returns a single planet"]]
```

**HPAPI Operations:**
```clojure
(martian/explore hpapi-martian)
;; => [[:characters "Return a list of characters"]
;;     [:spells "Return a list of spells"]]
```

### 2. Parameter Schema Detection

**List operation (optional params):**
```clojure
(-> (martian/explore swapi-martian :people) :parameters keys)
;; => (#schema.core.OptionalKey{:k :search} 
;;     #schema.core.OptionalKey{:k :page})
```

**Single entity operation (required param):**
```clojure
(-> (martian/explore swapi-martian :person) :parameters keys)
;; => (:id)  ;; No OptionalKey wrapper = required
```

**No params:**
```clojure
(-> (martian/explore hpapi-martian :spells) :parameters)
;; => {}
```

### 3. Request Execution

**SWAPI search (paginated response):**
```clojure
@(martian/response-for swapi-martian :people {:search "luke"})
;; => {:status 200
;;     :body {:count 1
;;            :next nil
;;            :previous nil
;;            :results [{:name "Luke Skywalker" :birth_year "19BBY" ...}]}
;;     :headers {...}}
```

**SWAPI single entity (direct object):**
```clojure
@(martian/response-for swapi-martian :person {:id "1"})
;; => {:status 200
;;     :body {:name "Luke Skywalker" :birth_year "19BBY" ...}
;;     :headers {...}}
```

**HPAPI (direct array):**
```clojure
@(martian/response-for hpapi-martian :spells)
;; => {:status 200
;;     :body [{:id "..." :name "Accio" :description "..."} ...]
;;     :headers {...}}
```

### 4. Debug Utilities

**Request preview:**
```clojure
(martian/request-for swapi-martian :people {:search "luke"})
;; => {:method :get
;;     :url "https://swapi.dev/api/people/"
;;     :query-params {:search "luke"}
;;     :headers {"Accept" "application/json"}}
```

**URL generation:**
```clojure
(martian/url-for swapi-martian :person {:id "1"})
;; => "https://swapi.dev/api/people/1"
```

---

## AI Guidance Effectiveness Analysis

### Strengths

1. **Clear Setup Section** - The require statements are copy-pasteable and work immediately
2. **Discovery Workflow** - The 3-step pattern (list → inspect → execute) is intuitive
3. **Response Structure Tables** - The comparison between SWAPI and HPAPI response formats prevents confusion
4. **Parameter Schema Explanation** - The `OptionalKey` notation is explained, critical for understanding required vs optional
5. **Real Examples** - Every code block produces actual output that matches documentation

### What Makes It AI-Friendly

1. **Incremental complexity** - Start with listing operations, then drill down
2. **No hidden state** - Mount states are auto-started on require
3. **Deref convention** - The `@` for promises is consistently shown
4. **Error response schemas** - 404 patterns documented in returns

### Minor Improvements (Optional)

1. **Add error handling example:**
```clojure
;; Handle non-existent entity
(let [{:keys [status body]} @(martian/response-for swapi-martian :person {:id "99999"})]
  (when (= status 404)
    (println "Not found:" (:detail body))))
```

2. **Add batch operations note:**
```clojure
;; Multiple concurrent requests
(let [luke (martian/response-for swapi-martian :person {:id "1"})
      vader (martian/response-for swapi-martian :person {:id "4"})]
  {:luke @luke :vader @vader})
```

3. **Add schema interpretation helper:**
```clojure
;; Quick param check helper
(defn required-params [martian op]
  (->> (-> (martian/explore martian op) :parameters keys)
       (remove #(instance? schema.core.OptionalKey %))
       (map keyword)))
       
(required-params swapi-martian :person) ;; => (:id)
(required-params swapi-martian :people) ;; => ()
```

---

## OpenAPI Spec Quality

### SWAPI (swapi.yml)

| Aspect | Quality | Notes |
|--------|---------|-------|
| Paths | ✅ Complete | All 12 endpoints (6 list + 6 single) |
| Parameters | ✅ Correct | search/page optional, id required |
| Response schemas | ⚠️ Verbose | Deep nesting due to $ref resolution |
| Operation IDs | ✅ Good | Intuitive names (:people, :person, etc.) |

### HPAPI (hpapi.yml)

| Aspect | Quality | Notes |
|--------|---------|-------|
| Paths | ✅ Complete | Both endpoints covered |
| Parameters | ✅ Correct | None required (correctly empty) |
| Response schemas | ✅ Concise | Direct array, no pagination wrapper |
| Operation IDs | ✅ Good | :characters, :spells |

---

## Component Architecture

### Mount State Dependencies

```
config (defaults.edn)
   └── swapi-martian (components/swapi.clj)
   └── hpapi-martian (components/hpapi.clj)
         └── interceptors (components/interceptors.clj)
```

### Interceptor Pipeline

```
[tap-response] → [default-interceptors] → [tap-request]
     ↑                                          ↓
  on leave                                  on enter
(logs response)                          (logs request)
```

---

## Recommendations

### For Documentation

1. **Keep the current structure** - It follows a logical discovery pattern
2. **Add a "Common Patterns" section** with:
   - Error handling
   - Concurrent requests
   - Parameter extraction utilities

### For Implementation

1. **Consider adding a helper namespace** (`us.whitford.facade.lib.martian`) with:
   - `list-operations` - prettier output of explore
   - `required-params` - extract required params only
   - `sample-request` - show full request without executing

### For Testing

The current setup supports effective REPL-driven testing. Consider adding:
- Property-based tests for schema validation
- Integration tests using VCR-style recorded responses

---

## Conclusion

**MARTIAN.md is production-ready for AI guidance.** An AI assistant can:

1. ✅ Discover available API operations
2. ✅ Understand required vs optional parameters
3. ✅ Execute requests and interpret responses
4. ✅ Debug requests without execution
5. ✅ Handle different response formats (paginated vs direct)

The documentation effectively bridges the gap between OpenAPI specs and runtime exploration, making it an excellent reference for both human developers and AI assistants.
