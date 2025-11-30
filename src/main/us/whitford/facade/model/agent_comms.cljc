(ns us.whitford.facade.model.agent-comms
  "Agent communication channel. Allows CLJS to send messages back to the CLJ REPL.
   
   Usage from CLJS:
   (require '[us.whitford.facade.model.agent-comms :as agent])
   (comp/transact! @SPA [(agent/send-message {:message \"hello\" :data {:foo 1}})])
   
   Read from CLJ:
   (require '[us.whitford.facade.model.agent-comms :refer [inbox]])
   @inbox  ;; vector of received messages
   (reset! inbox [])  ;; clear inbox"
  (:require
   [com.fulcrologic.fulcro.mutations :as m]
   [com.wsscode.pathom3.connect.operation :as pco]
   #?(:clj [taoensso.timbre :as log])))

#?(:clj (defonce inbox (atom [])))

#?(:clj
   (pco/defmutation send-message [{:keys [message data]}]
     {::pco/output [:agent/received]}
     (log/info "Agent message received:" message data)
     (swap! inbox conj {:message message :data data :timestamp (java.time.Instant/now)})
     {:agent/received true})
   :cljs
   (m/defmutation send-message [params]
     (remote [env] true)))

#?(:clj (def resolvers [send-message]))
