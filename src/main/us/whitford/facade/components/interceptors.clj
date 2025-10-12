(ns us.whitford.facade.components.interceptors
  "shared martian interceptors"
  (:require
    [clojure.pprint :refer [pprint]]
    [us.whitford.facade.components.config :refer [config]]
    [us.whitford.facade.components.utils :refer [json->data]]))

(def deadlock-guard
  "inject httpkit deadlock-guard? setting to the request on enter."
  {:name ::deadlock-guard
   :enter (fn [ctx]
            ; disabled not needed for promise
            #_(assoc-in ctx [:request :deadlock-guard?] false)
            ctx)})

(def increased-timeout
  "inject httpkit timeout setting to the request on enter"
  {:name ::increased-timeout
   :enter (fn [ctx]
            ; 10m timeout (reasoning models are slower)
            (assoc-in ctx [:request :timeout] 1000000))})

(def tap-request
  "martian interceptor to tap the request on enter"
  {:name ::tap-request
   :enter (fn [{:keys [request] :as ctx}]
            (tap> {:from ::tap-request :request (update request :body json->data) :ctx ctx})
            ctx)})

(def tap-response
  "martian interceptor to tap the response on leave"
  {:name ::tap-response
   :leave (fn [{:keys [response] :as ctx}]
            (tap> {:from ::tap-response :response response :ctx ctx})
            ctx)})
