(ns us.whitford.facade.components.statecharts
  (:require [com.fulcrologic.statecharts :as sc]
            [com.fulcrologic.statecharts.chart :refer [statechart]]
            [com.fulcrologic.statecharts.data-model.operations :as ops]
            [com.fulcrologic.statecharts.elements :refer [state final parallel transition on-entry on-exit
                                                          script]]
            [com.fulcrologic.statecharts.event-queue.core-async-event-loop :as scloop]
            [com.fulcrologic.statecharts.events :refer [cancel-event]]
            [com.fulcrologic.statecharts.simple :as simple]
            [mount.core :refer [defstate]]
            [us.whitford.facade.components.parser :refer [parser]]))

(set! *warn-on-reflection* true)

(def swapi-statechart
  (statechart {:name "swapi"
               :initial :swapi}
    (state {:id :swapi
            :initial :swapi/start}
      (transition {:id :transition/swapi->swapi-end
                   :event :cancel
                   :target :swapi/end})
      (state {:id :swapi/start}
        (on-exit {:id :exit/swapi-start}
          (script {:id :script/exit-swapi-start
                   :expr (fn exit-swapi-start [env data]
                           (tap> {:from ::exit-swapi-start :env env :data data}))}))
        (transition {:id :transition/swapi-start->swapi-running
                     :target :swapi/running}))
      (state {:id :swapi/running})
      (final {:id :swapi/end})))
  )

(defstate statecharts
  :start
  (let [env (simple/simple-env {})
        running? (scloop/run-event-loop! env 100)
        charts [(assoc swapi-statechart :session-id :swapi)]]
    (doseq [sc charts]
           (let [{:keys [session-id]} sc]
             (simple/register! env session-id sc)  ; register in env
             (simple/start! env session-id session-id) ; start statechart on the event-loop
             (simple/send! env {:event :start :delay 1000 :target session-id})))
    {:env env
     :running? running?
     :charts charts})
  :stop
  (let [{:keys [env running? charts]} statecharts]
    (doseq [sc charts]
           (let [{:keys [session-id]} sc]
             (simple/send! env {:target session-id :delay 500 :event :exit})  ; send exit event
             (simple/send! env {:target session-id :delay 1500 :event cancel-event})))
    (Thread/sleep 3000)
    (reset! running? false)
    (println "statecharts stopped")))

(tap> statecharts)
