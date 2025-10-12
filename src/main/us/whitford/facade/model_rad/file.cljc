(ns us.whitford.facade.model-rad.file
  "RAD definition of a `file`. Attributes only. These will be used all over the app, so try to limit
   requires to model code and library code."
  (:require
    [com.fulcrologic.rad.attributes :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.blob :as blob]))

(defattr id :file/id :uuid
  {ao/identity? true
   ao/schema    :production})

(blob/defblobattr sha :file/sha :files :remote
  {ao/identities #{:file/id}
   ao/schema     :production})

(defattr filename :file.sha/filename :string
  {ao/identities #{:file/id}
   ao/schema     :production})

(defattr uploaded-on :file/uploaded-on :instant
  {ao/identities #{:file/id}
   ao/schema     :production})

(def attributes [id sha filename uploaded-on])
