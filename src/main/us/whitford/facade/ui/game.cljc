(ns us.whitford.facade.ui.game
  "Whack-a-Toast game - click toasts before they disappear!"
  (:require
   #?@(:cljs [[us.whitford.facade.ui.toast :as toast]
              [us.whitford.facade.application :refer [SPA]]
              [us.whitford.facade.model.agent-comms :as agent]
              [com.fulcrologic.fulcro.components :as comp]])))

#?(:cljs
   (do
     (defonce game-state (atom {:round 0
                                :score 0
                                :misses 0
                                :reaction-times []
                                :toast-counter 0
                                :active-toasts 0
                                :round-complete? true}))

     (defn- send-result! [message data]
       (comp/transact! @SPA [(agent/send-message {:message message :data data})]))

     (defn- game-stats []
       (let [{:keys [score misses reaction-times]} @game-state
             total (+ score misses)
             avg (when (seq reaction-times)
                   (Math/round (/ (reduce + reaction-times) (count reaction-times))))]
         {:score score :misses misses :total total :avg-ms avg}))

     (defn- show-results! []
       (let [{:keys [score misses total avg-ms]} (game-stats)
             pct (if (pos? total) (Math/round (* 100 (/ score total))) 0)]
         (toast/toast!
          #js {:autoClose 5000
               :position "top-center"
               :type "info"}
          (str "🏁 Round over! Score: " score "/" total " (" pct "%) | Avg: " (or avg-ms "N/A") "ms"))
         (send-result! "ROUND_COMPLETE" (game-stats))))

     (defn- on-round-complete! []
       (when (and (not (:round-complete? @game-state))
                  (zero? (:active-toasts @game-state)))
         (swap! game-state assoc :round-complete? true)
         (let [{:keys [round]} @game-state]
           (if (< round 2)
             ;; Start next round after delay
             (js/setTimeout
              (fn []
                (toast/toast! #js {:autoClose 2000 :position "top-center" :type "warning"}
                              "⚡ Round 2 - FASTER!")
                (js/setTimeout #(start-round! 5 1000 350) 2500))
              1500)
             ;; Game over - show final results
             (do
               (show-results!)
               (send-result! "GAME_COMPLETE" (game-stats)))))))

     (defn- spawn-toast! [auto-close-ms]
       (let [id (swap! game-state update :toast-counter inc)
             id-val (:toast-counter @game-state)
             start-time (js/Date.now)
             hit? (atom false)]
         (swap! game-state update :active-toasts inc)
         (toast/toast!
          #js {:autoClose auto-close-ms
               :position "top-center"
               :closeOnClick false
               :onClick (fn []
                          (when-not @hit?
                            (reset! hit? true)
                            (let [reaction-ms (- (js/Date.now) start-time)]
                              (swap! game-state (fn [s]
                                                  (-> s
                                                      (update :score inc)
                                                      (update :reaction-times conj reaction-ms))))
                              (send-result! "HIT" {:reaction-ms reaction-ms :toast-id id-val}))))
               :onClose (fn []
                          (swap! game-state update :active-toasts dec)
                          (when-not @hit?
                            (swap! game-state update :misses inc)
                            (send-result! "MISS" {:toast-id id-val}))
                          (on-round-complete!))}
          (str "🎯 #" id-val))))

     (defn start-round! [n auto-close-ms spacing-ms]
       (swap! game-state assoc :round-complete? false)
       (swap! game-state update :round inc)
       (dotimes [i n]
         (js/setTimeout #(spawn-toast! auto-close-ms) (* i spacing-ms))))

     (defn start-game! []
       (reset! game-state {:round 0
                           :score 0
                           :misses 0
                           :reaction-times []
                           :toast-counter 0
                           :active-toasts 0
                           :round-complete? true})
       (send-result! "GAME_START" {})
       (toast/toast! #js {:autoClose 2000 :position "top-center" :type "success"}
                     "🎮 Whack-a-Toast! Click the targets!")
       ;; Start round 1 after intro toast
       (js/setTimeout #(start-round! 5 1800 500) 2500))))
