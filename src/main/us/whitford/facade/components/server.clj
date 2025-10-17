(ns us.whitford.facade.components.server
  (:require
    [mount.core :as mount :refer [defstate]]
    [org.httpkit.server :refer [run-server]]
    [taoensso.timbre :as log]
    [us.whitford.facade.components.config :refer [config]]
    [us.whitford.facade.components.ring-middleware :refer [middleware]]
    [us.whitford.facade.components.statecharts :refer [statecharts]]))

;; expose statecharts to the compiler so mount will autostart the state
(comment
  (:env statecharts))

(defstate http-server
  :start
  (let [cfg     (get config :org.httpkit.server/config)
        stop-fn (run-server middleware cfg)]
    (log/info "Starting webserver with config " cfg)
    {:stop stop-fn})
  :stop
  (let [{:keys [stop]} http-server]
    (when stop
          (stop))))

;; This is a separate file for the uberjar only. We control the server in dev mode from src/dev/user.clj
(defn -main [& args]
  (mount/start-with-args {:config "config/prod.edn"}))
