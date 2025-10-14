(ns us.whitford.facade.model-rad.attributes
  "Central place to gather all RAD attributes to ensure they get required and
   stay required.

   Also defines common helpful things related to the attributes of the model, such
   as a default form validator and attribute lookup."
  (:require
    [com.fulcrologic.rad.attributes :as attr]
    [us.whitford.facade.model-rad.account :as account]
    [us.whitford.facade.model-rad.file :as m.file]
    [us.whitford.facade.model-rad.swapi :as m.swapi]))

(def all-attributes (into []
                      (concat
                        account/attributes
                        m.file/attributes
                        m.swapi/attributes)))

(def key->attribute (attr/attribute-map all-attributes))

(def all-attribute-validator (attr/make-attribute-validator all-attributes))
