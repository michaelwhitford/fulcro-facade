(ns us.whitford.facade.ui.toast
  #?(:cljs
     (:require ["react-toastify" :refer [ToastContainer toast]]
               [com.fulcrologic.fulcro.dom :as dom]
               [com.fulcrologic.fulcro.components :as comp]
               [us.whitford.facade.application :refer [SPA]]
               [us.whitford.facade.model.agent-comms :as agent])))

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
     "Ask a yes/no question via toast. Answer sent to agent-comms inbox.
      
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
                            (comp/transact! @SPA [(agent/send-message
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
