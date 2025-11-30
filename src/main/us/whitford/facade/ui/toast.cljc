(ns us.whitford.facade.ui.toast
  #?(:cljs
     (:require ["react-toastify" :as toastify :refer [ToastContainer toast]]
               [com.fulcrologic.fulcro.application :as app]
               [com.fulcrologic.fulcro.dom :as dom]
               [com.fulcrologic.fulcro.components :as comp]
               [com.fulcrologic.fulcro.data-fetch :as df]
               [us.whitford.facade.application :refer [SPA]]
               [us.whitford.facade.model.agent-comms :as agent-comms]
               [us.whitford.facade.model.prompt :as prompt]
               [taoensso.timbre :as log])))

;; Re-export for game.cljc to use in custom toast content
#?(:cljs (def raw-toast toast))
#?(:cljs (def dom-div dom/div))
#?(:cljs (def dom-button dom/button))

(defn ui-toast-container

  "Embed the toast container. Must be placed somewhere near the root where it will always be rendered.
   props are as described in https://fkhadra.github.io/react-toastify/introduction

   * :position - one of top-right, top-left, top-center, bottom-*
   * :autoClose - ms until closing (5000)
   * :hideProgressBar - Default false
   * :newestOnTop - Default false
   * :closeOnClick - Default true
   * :rtl - Default false
   * :pauseOnFocusLoss - Default true
   * :draggable - Default true
   * :pauseOnHover - Default true

   These can be overridden in the trigger function `toast!`.
   "
  ([props]
   #?(:cljs (dom/create-element ToastContainer (clj->js props))))
  ([]
   #?(:cljs (dom/create-element ToastContainer))))

(defn toast!
  "Open a toast in the given toast container. Default options are specified on that container.
   props can override the options (described at https://fkhadra.github.io/react-toastify/introduction)
   * :position - one of top-right, top-left, top-center, bottom-*
   * :autoClose - ms until closing
   * :hideProgressBar
   * :newestOnTop
   * :closeOnClick
   * :rtl
   * :pauseOnFocusLoss
   * :draggable
   * :pauseOnHover
  "
  ([props message]
   #?(:cljs (toast message (clj->js props))))
  ([message]
   #?(:cljs (toast message))))

#?(:cljs
   (defn ask!
     "Ask a yes/no question via toast. Answer sent to agent-comms inbox (legacy).
      
      Usage from CLJS REPL:
      (require '[us.whitford.facade.ui.toast :refer [ask!]])
      (ask! \"Continue with deployment?\")
      
      Read answer in CLJ REPL:
      (last @us.whitford.facade.model.agent-comms/inbox)
      ;; => {:message \"ANSWER\", :data {:question \"...\" :answer true/false}, ...}"
     [question]
     (let [toast-id (atom nil)
           answered? (atom false)
           send-answer! (fn [answer]
                          (when (compare-and-set! answered? false true)
                            (.dismiss toast @toast-id)
                            (comp/transact! @SPA [(agent-comms/send-message
                                                    {:message "ANSWER"
                                                     :data {:question question :answer answer}})])))]
       (reset! toast-id
         (toast
           (fn [_props]
             (dom/div {}
               (dom/div {:style {:marginBottom "10px" :fontWeight "bold"}} question)
               (dom/div {:style {:display "flex" :gap "8px"}}
                 (dom/button {:className "ui green mini button"
                              :onClick #(send-answer! true)} "Yes")
                 (dom/button {:className "ui red mini button"
                              :onClick #(send-answer! false)} "No"))))
           #js {:autoClose false
                :closeOnClick false
                :draggable false
                :position "top-center"})))))

#?(:cljs
   (defn ask-statechart!
     "Ask a yes/no question via toast, integrated with statechart.
      
      This is the new statechart-based approach. The answer is sent back
      to the CLJ statechart via the prompt/answer-question mutation.
      
      Used internally by poll-for-questions! when CLJ asks a question."
     [session-id question]
     (let [toast-id (atom nil)
           answered? (atom false)
           send-answer! (fn [answer]
                          (when (compare-and-set! answered? false true)
                            (log/debug "Sending answer to CLJ:" session-id answer)
                            (.dismiss toast @toast-id)
                            (comp/transact! @SPA [(prompt/answer-question
                                                    {:session-id session-id
                                                     :answer answer})])))]
       (reset! toast-id
         (toast
           (fn [_props]
             (dom/div {}
               (dom/div {:style {:marginBottom "10px" :fontWeight "bold"}} question)
               (dom/div {:style {:display "flex" :gap "8px"}}
                 (dom/button {:className "ui green mini button"
                              :onClick #(send-answer! true)} "Yes")
                 (dom/button {:className "ui red mini button"
                              :onClick #(send-answer! false)} "No"))))
           #js {:autoClose false
                :closeOnClick false
                :draggable false
                :position "top-center"})))))

;; ============================================================================
;; Polling for CLJ questions
;; ============================================================================

#?(:cljs
   (defonce shown-questions (atom #{})))

#?(:cljs
   (defonce poll-interval-id (atom nil)))

#?(:cljs
   (defn handle-pending-questions
     "Process pending questions from CLJ, showing toasts for new ones."
     [questions]
     (doseq [{:prompt.question/keys [session-id question]} questions]
       (when-not (contains? @shown-questions session-id)
         (swap! shown-questions conj session-id)
         (log/info "Showing prompt question:" session-id question)
         (ask-statechart! session-id question)))))

#?(:cljs
   (defn poll-for-questions!
     "Fetch pending questions from CLJ and show toasts for any new ones.
      Reads from app state after load completes."
     []
     ;; Load the questions - they get merged into app state automatically
     (df/load! @SPA :prompt/pending-questions nil)
     ;; After a short delay, check the app state for questions
     ;; (the load is async, so we wait a bit for it to complete)
     (js/setTimeout
       (fn []
         (let [state-map (app/current-state @SPA)
               questions (get state-map :prompt/pending-questions)]
           (when (seq questions)
             (log/debug "Found pending questions:" (count questions))
             (handle-pending-questions questions))))
       200)))

#?(:cljs
   (defn start-polling!
     "Start polling for CLJ questions. Called once at app startup.
      
      Options:
      - :interval-ms - How often to poll (default: 5000ms)"
     ([] (start-polling! {}))
     ([{:keys [interval-ms] :or {interval-ms 5000}}]
      (when @poll-interval-id
        (js/clearInterval @poll-interval-id))
      (log/info "Starting prompt question polling every" interval-ms "ms")
      (reset! poll-interval-id
        (js/setInterval poll-for-questions! interval-ms))
      ;; Do an immediate poll
      (poll-for-questions!))))

#?(:cljs
   (defn stop-polling!
     "Stop polling for CLJ questions."
     []
     (when @poll-interval-id
       (log/info "Stopping prompt question polling")
       (js/clearInterval @poll-interval-id)
       (reset! poll-interval-id nil))))
