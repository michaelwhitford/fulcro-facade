(ns us.whitford.facade.model.account
  "Functions, resolvers, and mutations supporting `account`.

   DO NOT require a RAD model file in this ns. This ns is meant to be an ultimate
   leaf of the requires. Only include library code."
  (:require
    #_[com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [com.fulcrologic.rad.database-adapters.datomic-options :as do]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [com.wsscode.pathom3.connect.operation :as pco]
    [datomic.client.api :as d]
    [taoensso.timbre :as log]))

(defn new-account
  "Create a new account. The Datomic tempid will be the email."
  [email & {:as addl}]
  (merge
    {:db/id           email
     :account/email   email
     :account/active? true
     :account/id      (new-uuid)}
    addl))

#?(:clj
   (defn get-all-accounts
     "Fetch all accounts from database. Returns empty vector on error."
     [env query-params]
     (try
       (if-let [db (some-> (get-in env [do/databases :production]) deref)]
         (let [ids (map first
                     (if (:show-inactive? query-params)
                         (d/q [:find '?uuid
                               :where
                               ['?dbid :account/id '?uuid]] db)
                         (d/q [:find '?uuid
                               :where
                               ['?dbid :account/active? true]
                               ['?dbid :account/id '?uuid]] db)))]
           (mapv (fn [id] {:account/id id}) ids))
         (do
           (log/error "No database atom for production schema!")
           []))
       (catch Exception e
         (log/error e "Failed to fetch accounts from database")
         []))))

#?(:clj
   (pco/defresolver all-accounts-resolver [env params]
     {::pco/output [{:account/all-accounts [:account/id]}]}
     (try
       {:account/all-accounts (or (get-all-accounts env params) [])}
       (catch Exception e
         (log/error e "Failed to resolve all-accounts")
         {:account/all-accounts []}))))

#?(:clj
   (pco/defresolver account-count-resolver
     "Count total accounts in the database.
      RADAR will auto-discover and use this resolver for entity counts!"
     [env _]
     {::pco/output [:account/count]}
     (try
       (if-let [db (some-> (get-in env [do/databases :production]) deref)]
         (let [result (d/q '[:find ?e
                             :where [?e :account/id]]
                           db)]
           {:account/count (count result)})
         {:account/count 0})
       (catch Exception e
         (log/error e "Failed to count accounts")
         {:account/count nil}))))

(def resolvers [all-accounts-resolver account-count-resolver])
