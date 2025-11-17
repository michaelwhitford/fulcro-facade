(ns us.whitford.facade.components.hpapi
  (:require
   [clojure.pprint :refer [pprint]]
   [martian.core :as martian]
   [martian.httpkit :as martian-http]
   [mount.core :refer [defstate]]
   [us.whitford.facade.components.config :refer [config]]
   [us.whitford.facade.components.interceptors :as interceptors]
   [us.whitford.facade.model-rad.attributes :refer [all-attributes]]))

(def image-encoder
  {;; unary function of request `:body`, Str -> Str}
   :encode (fn [req] "")
   ;; unary fn of response `:body`, Str -> Str
   :decode (fn [res] "")
   :as :string})

(defstate hpapi-martian
  :start
  (let [{:keys [swagger-file server-url]} (get config ::config)]
    (martian-http/bootstrap-openapi
     swagger-file
     {:server-url server-url
      :interceptors (vec (concat
                          [interceptors/tap-response]
                          martian-http/default-interceptors
                          [interceptors/tap-request]))})))

;
;       :request-encoders request-encoders
;       :response-encoders response-encoders
;        request-encoders (assoc martian-http/default-request-encoders
;                                "image/*" image-encoder)
;        response-encoders (assoc martian-http/default-response-encoders
;                                 "image/*" image-encoder)]
(comment
  (pprint (::config config))
  (tap> hpapi-martian)
  (martian/explore hpapi-martian :characters)
  (martian/explore hpapi-martian :spells)
  @(martian/response-for hpapi-martian :characters)
  @(martian/response-for hpapi-martian :spells))
