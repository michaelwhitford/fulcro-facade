(ns us.whitford.facade.components.statecharts
  (:require [com.fulcrologic.statecharts :as sc]
            [com.fulcrologic.statecharts.chart :refer [statechart]]
            [com.fulcrologic.statecharts.data-model.operations :as ops]
            [com.fulcrologic.statecharts.elements :refer [state final parallel transition on-entry on-exit
                                                          script invoke]]
            [com.fulcrologic.statecharts.event-queue.core-async-event-loop :as scloop]
            [com.fulcrologic.statecharts.events :refer [cancel-event]]
            [com.fulcrologic.statecharts.simple :as simple]
            [datomic.client.api :as d]
            [martian.core :as martian]
            [mount.core :refer [defstate]]
            [us.whitford.facade.components.config :refer [config]]
            [us.whitford.facade.components.parser :refer [parser]]
            [us.whitford.facade.components.utils :refer [map->nsmap]]
            [us.whitford.facade.model.swapi :refer [get-swapi transform-swapi]]))

(set! *warn-on-reflection* true)

(def etl-statechart
  (statechart {:name "etl"
               :initial :etl}
    (state {:id :etl
            :initial :etl/idle}
      (transition {:target :etl/end
                   :event :exit})
      (state {:id :etl/idle}
        (transition {:target :etl/extract
                     :event :etl/process}))
      (state {:id :etl/extract}
        (transition {:target :etl/transform}))
      (state {:id :etl/transform}
        (transition {:target :etl/load}))
      (state {:id :etl/load}
        (transition {:target :etl/idle}))
      (final {:id :etl/end}))))

(def iteration-statechart
  (statechart {:name "iteration"}
    (state {:id :iteration
            :initial :iteration/idle}
      (state {:id :iteration/idle}
        (transition {:id :transition/iteration-idle->paginate-init
                     :event :iteration
                     :target :iteration/init}))
      (state {:id :iteration/init})
      (state {:id :iteration/stepfn})
      (state {:id :iteration/valuefn})
      (state {:id :iteration/continuationfn})
      (state {:id :iteration/somefn})
      )))

(def swapi-statechart
  (statechart {:name "swapi"
               :initial :swapi}
    (state {:id :swapi
            :initial :swapi/start}
      (transition {:id :transition/swapi->swapi-end
                   :event :exit
                   :target :swapi/end})
      (state {:id :swapi/start}
        (on-entry {:id :entry/swapi-start}
          (script {:id :script/entry-swapi-start
                   :expr (fn entry-swapi-start [env data]
                           (let [cfg (:us.whitford.facade.components.swapi/config config)
                                 entities {:people :person
                                           :vehicles :vehicle
                                           :starships :starship
                                           :planets :planet
                                           :films :film
                                           :species :specie}]
                             [(ops/assign [:swapi :config] cfg)
                              (ops/assign [:swapi :entities] entities)
                              (ops/assign [:swapi :entitytypes] (set (keys entities)))
                              (ops/assign [:swapi :valuetypes] (set (vals entities)))]))}))
        (on-exit {:id :exit/swapi-start})
        (transition {:id :transition/swapi-start->swapi-running
                     :target :swapi/running}))

      (parallel {:id :swapi/running}
        (transition {:id :transition/swapi-running->running-fetc
                     :event :fetch
                     :target :fetch/processing})

        (state {:id :running/fetch
                :initial :fetch/idle}
          (state {:id :fetch/idle})
          (state {:id :fetch/processing}
            (invoke {:type :future
                     :params (fn fetch-processing-params [env {:keys [_event] :as data}]
                               (let [{:keys [entitytype] :or {entitytype :none}} (:data _event)]
                                 #_(tap> {:from ::fetch-processing-params :env env :data data})
                                 {:entitytype entitytype}))
                     :src (fn fetch-processing [{:keys [entitytype] :as params}]
                            #_(tap> {:from ::load-processing :params params})
                            {:entitytype entitytype
                             :results (get-swapi params)})})
            (transition {:id :transition/fetch-processing-future-done
                         :event :done.invoke.*}
              (script {:id :script/fetch-processing-future-done
                       :expr (fn fetch-processing-future-done [env {:keys [_event] :as data}]
                               (tap> {:from ::fetch-processing-future-done :env env :data data})
                               (simple/send! env {:event :etl
                                                  :data (:data _event)
                                                  :target :swapi}))}))))

        (state {:id :running/etl
                :initial :etl/idle}
          (state {:id :etl/idle}
            (transition {:id :transition/etl-idle->etl-processing
                         :event :etl
                         :target :etl/processing})
            (on-entry {:id :entry/etl-idle}
              (script {:id :script/entry-etl-idle
                       :expr (fn entry-etl-idle [env data]
                               (tap> {:from ::entry-etl-idle :env env :data data}))})))

          (state {:id :etl/processing}
            (on-entry {:id :entry/etl-processing}
              (script {:id :script/entry-etl-processing
                       :expr (fn entry-etl-processing [env {:keys [_event swapi] :as data}]
                               (tap> {:from ::entry-etl-processing :env env :data data})
                               (let [{:keys [entitytype results]} (:data _event)
                                     {:keys [entities]} swapi]
                                 (when results
                                       (let [transformed (->> results
                                                              transform-swapi
                                                              (mapv #(map->nsmap % (name (entities entitytype)))))]
                                         [(ops/assign [:swapi entitytype] transformed)]))))}))
            (transition {:id :transition/etl-processing->etl-idle
                         :target :etl/idle}))))

      (final {:id :swapi/end}))))

(defstate statecharts
  :start
  (let [env (simple/simple-env {})
        running? (scloop/run-event-loop! env 100)
        charts [(assoc swapi-statechart :session-id :swapi :chart-id ::swapi)]]
    (doseq [sc charts]
           (let [{:keys [chart-id session-id]} sc]
             (simple/register! env chart-id sc)  ; register in env
             (simple/start! env chart-id session-id) ; start statechart on the event-loop
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

(comment
  (tap> statecharts)
  (doseq [e [:people :films :vehicles :starships :planets :species]]
         (simple/send! (:env statecharts) {:target :swapi
                                           :event :fetch
                                           :delay (rand-int 10001)
                                           :data {:entitytype e}}))
  )
