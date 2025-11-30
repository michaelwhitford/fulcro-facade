(ns us.whitford.facade.model.prompt
  "Statechart-based user prompts. Ask yes/no questions from CLJ REPL, get answers from browser.
   
   ## Why Statecharts?
   
   The original inbox approach required manual polling and had no state tracking.
   This statechart provides:
   - Explicit states: :idle, :awaiting-response, :completed
   - Timeout support (configurable)
   - Clean request/response correlation via session-id
   - Observable state for debugging
   
   ## Usage from CLJ REPL
   
   ```clj
   (require '[us.whitford.facade.model.prompt :as prompt])
   
   ;; Ask a yes/no question (returns immediately with session-id)
   (def q (prompt/ask! \"Deploy to production?\"))
   ;; => {:session-id :prompt/12345 :status :awaiting-response}
   
   ;; Poll for completion (or check :status)
   (prompt/get-result q)
   ;; => {:status :completed :answer true} or {:status :awaiting-response} or {:status :timeout}
   ```
   
   ## Usage from CLJS (internal - toast integration)
   
   The toast UI sends :event/answer to the statechart when user clicks Yes/No."
  (:require
   #?(:clj [com.fulcrologic.statecharts :as sc])
   #?(:clj [com.fulcrologic.statecharts.chart :refer [statechart]])
   #?(:clj [com.fulcrologic.statecharts.data-model.operations :as ops])
   #?(:clj [com.fulcrologic.statecharts.elements :refer [state final transition on-entry script Send]])
   #?(:clj [com.fulcrologic.statecharts.convenience :refer [send-after on]])
   #?(:clj [com.fulcrologic.statecharts.simple :as simple])
   #?(:clj [com.fulcrologic.statecharts.event-queue.core-async-event-loop :as scloop])
   #?(:clj [mount.core :refer [defstate]])
   #?(:clj [taoensso.timbre :as log])
   [com.fulcrologic.fulcro.mutations :as m]
   [com.wsscode.pathom3.connect.operation :as pco]))

;; ============================================================================
;; CLJ: Statechart Definition & Runtime
;; ============================================================================

#?(:clj
   (def ask-statechart
     "A simple statechart for yes/no confirmation flow.
      
      States:
      - :ask/idle       - Initial state, waiting to start
      - :ask/pending    - Question sent to browser, awaiting response
      - :ask/completed  - User answered (result in data model)
      - :ask/timeout    - No response within timeout period
      - :ask/cancelled  - Explicitly cancelled"
     (statechart {:id :ask-chart}
       (state {:id :ask
               :initial :ask/idle}

         ;; Idle: waiting for a question to be asked
         (state {:id :ask/idle}
           (transition {:event :event/ask
                        :target :ask/pending}
             (script {:expr (fn [_env {:keys [_event]}]
                              (let [{:keys [question timeout-ms]} (:data _event)]
                                [(ops/assign :question question)
                                 (ops/assign :timeout-ms (or timeout-ms 60000))
                                 (ops/assign :asked-at (System/currentTimeMillis))]))})))

         ;; Pending: question shown in browser, waiting for user response
         (state {:id :ask/pending}
           (on-entry {}
             ;; Schedule timeout event
             (Send {:id :timeout-send
                    :event :event/timeout
                    :delayexpr (fn [_env data] (:timeout-ms data))}))

           ;; User answered - transition to completed
           (transition {:event :event/answer
                        :target :ask/completed}
             (script {:expr (fn [_env {:keys [_event]}]
                              [(ops/assign :answer (:answer (:data _event)))
                               (ops/assign :completed-at (System/currentTimeMillis))])}))

           ;; Timeout fired
           (transition {:event :event/timeout
                        :target :ask/timeout})

           ;; Explicit cancel
           (transition {:event :event/cancel
                        :target :ask/cancelled}))

         ;; Terminal states
         (final {:id :ask/completed})
         (final {:id :ask/timeout})
         (final {:id :ask/cancelled})))))

#?(:clj
   (defonce prompt-env (atom nil)))

#?(:clj
   (defonce pending-prompts (atom {})))

#?(:clj
   (defstate prompt-statecharts
     :start
     (let [env (simple/simple-env {})
           running? (scloop/run-event-loop! env 100)]
       (simple/register! env :ask-chart ask-statechart)
       (reset! prompt-env env)
       (log/info "Prompt statecharts initialized")
       {:env env
        :running? running?})
     :stop
     (let [{:keys [running?]} prompt-statecharts]
       (reset! running? false)
       (reset! prompt-env nil)
       (reset! pending-prompts {})
       (log/info "Prompt statecharts stopped"))))

#?(:clj
   (defn ask!
     "Ask a yes/no question. Returns a map with :session-id for tracking.
      
      Options:
      - :timeout-ms - How long to wait for response (default: 60000ms)
      
      Returns: {:session-id <keyword> :status :awaiting-response}
      
      The question will be sent to the browser via a Pathom mutation.
      Poll with (get-result {:session-id ...}) to check for answer."
     ([question] (ask! question {}))
     ([question {:keys [timeout-ms] :or {timeout-ms 60000}}]
      (when-not @prompt-env
        (throw (ex-info "Prompt statecharts not started. Is mount/start called?" {})))
      (let [session-id (keyword "prompt" (str (random-uuid)))
            env @prompt-env]
        ;; Start a new statechart session for this question
        (simple/start! env :ask-chart session-id)
        ;; Send the ask event to transition to pending
        (simple/send! env {:target session-id
                           :event :event/ask
                           :data {:question question
                                  :timeout-ms timeout-ms}})
        ;; Track this pending prompt
        (swap! pending-prompts assoc session-id {:question question
                                                  :status :awaiting-response
                                                  :started-at (System/currentTimeMillis)})
        {:session-id session-id
         :status :awaiting-response}))))

#?(:clj
   (defn receive-answer!
     "Called when the browser sends back an answer. Internal use."
     [session-id answer]
     (when-let [env @prompt-env]
       (simple/send! env {:target session-id
                          :event :event/answer
                          :data {:answer answer}})
       (swap! pending-prompts update session-id assoc
              :status :completed
              :answer answer
              :completed-at (System/currentTimeMillis))
       {:received true :session-id session-id :answer answer})))

#?(:clj
   (defn get-result
     "Get the current status/result of a prompt.
      
      Returns:
      - {:status :awaiting-response} - Still waiting
      - {:status :completed :answer true/false} - User answered
      - {:status :timeout} - Timed out
      - {:status :not-found} - Unknown session-id"
     [{:keys [session-id]}]
     (if-let [prompt-data (get @pending-prompts session-id)]
       prompt-data
       {:status :not-found})))

#?(:clj
   (defn pending-questions
     "List all pending questions (for debugging)."
     []
     (->> @pending-prompts
          (filter (fn [[_ v]] (= :awaiting-response (:status v))))
          (into {}))))

;; ============================================================================
;; Pathom: Browser <-> CLJ communication
;; ============================================================================

#?(:clj
   (pco/defresolver pending-questions-resolver [_env _input]
     {::pco/output [{:prompt/pending-questions [:prompt.question/session-id
                                                 :prompt.question/question]}]}
     {:prompt/pending-questions
      (->> @pending-prompts
           (filter (fn [[_ v]] (= :awaiting-response (:status v))))
           (mapv (fn [[session-id {:keys [question]}]]
                   ;; Use str to preserve namespace (name strips it!)
                   {:prompt.question/session-id (subs (str session-id) 1) ;; remove leading :
                    :prompt.question/question question})))}))

#?(:clj
   (pco/defmutation answer-question [{:keys [session-id answer]}]
     {::pco/output [:prompt/received :tempids]}
     (log/info "Prompt answer received:" session-id answer)
     (receive-answer! (keyword session-id) answer)
     {:prompt/received true :tempids {}})
   :cljs
   (m/defmutation answer-question [_params]
     (remote [_env] true)))

#?(:clj (def resolvers [pending-questions-resolver answer-question]))
