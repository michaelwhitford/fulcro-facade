(ns us.whitford.facade.components.wttr
  (:require
    [clojure.pprint :refer [pprint]]
    [martian.core :as martian]
    [martian.httpkit :as martian-http]
    [mount.core :refer [defstate]]
    [us.whitford.facade.components.config :refer [config]]
    [us.whitford.facade.components.interceptors :as interceptors]))

(defstate wttr-martian
  :start
  (let [{:keys [swagger-file server-url]} (get config :us.whitford.facade.components.wttr/config)]
    (martian-http/bootstrap-openapi
      swagger-file
      {:server-url server-url
       :interceptors (vec (concat
                            [interceptors/tap-response]
                            martian-http/default-interceptors
                            [interceptors/tap-request]))})))

(comment
  (pprint config)
  (tap> wttr-martian)
  (martian/explore wttr-martian)
  (martian/explore wttr-martian :forecast)
  @(martian/response-for wttr-martian :forecast {:location "London" :format "j1"}))
