(ns us.whitford.facade.components.ipapi
  (:require
    [clojure.pprint :refer [pprint]]
    [martian.core :as martian]
    [martian.httpkit :as martian-http]
    [mount.core :refer [defstate]]
    [us.whitford.facade.components.config :refer [config]]
    [us.whitford.facade.components.interceptors :as interceptors]))

(defstate ipapi-martian
  :start
  (let [{:keys [swagger-file server-url]} (get config :us.whitford.facade.components.ipapi/config)]
    (martian-http/bootstrap-openapi
      swagger-file
      {:server-url server-url
       :interceptors (vec (concat
                            [interceptors/tap-response]
                            martian-http/default-interceptors
                            [interceptors/tap-request]))})))

(comment
  (pprint config)
  (tap> ipapi-martian)
  (martian/explore ipapi-martian)
  (martian/explore ipapi-martian :ip-lookup)
  @(martian/response-for ipapi-martian :ip-lookup {:ip "8.8.8.8"}))
