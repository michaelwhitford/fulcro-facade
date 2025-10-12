(ns us.whitford.facade.ui.file-forms
  (:require
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [us.whitford.facade.model-rad.file :as r.file]))

(form/defsc-form FileForm [this props]
  {fo/id            r.file/id
   fo/layout-styles {:form-container :file-as-icon}
   fo/attributes    [r.file/uploaded-on
                     r.file/sha
                     r.file/filename]})
