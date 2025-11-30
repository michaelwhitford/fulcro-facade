(ns us.whitford.facade.ui.game
  "Whack-a-Toast and Tic-Tac-Toast games!"
  #?(:cljs
     (:require
      [us.whitford.facade.ui.toast :as toast]
      [us.whitford.facade.application :refer [SPA]]
      [us.whitford.facade.model.agent-comms :as agent]
      [com.fulcrologic.fulcro.components :as comp])))

#?(:cljs
   (do
     (declare start-round!)
     
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
       (js/setTimeout #(start-round! 5 1800 500) 2500))

     ;; ============================================================
     ;; Tic-Tac-Toast - Play tic-tac-toe via toasts!
     ;; ============================================================

     (defonce ttt-state (atom {:board [nil nil nil nil nil nil nil nil nil]
                               :game-over? true}))

     (defn- ttt-check-winner [board]
       (let [lines [[0 1 2] [3 4 5] [6 7 8]   ;; rows
                    [0 3 6] [1 4 7] [2 5 8]   ;; cols
                    [0 4 8] [2 4 6]]]         ;; diagonals
         (some (fn [line]
                 (let [vals (map #(get board %) line)]
                   (when (and (apply = vals) (first vals))
                     (first vals))))
               lines)))

     (defn- ttt-board-full? [board]
       (every? some? board))

     (defn- ttt-available [board]
       (vec (keep-indexed (fn [i v] (when (nil? v) i)) board)))

     (defn- ttt-ai-move [board]
       ;; Simple AI: center, corners, then edges
       (let [avail (set (ttt-available board))]
         (cond
           (avail 4) 4
           (some avail [0 2 6 8]) (first (filter avail [0 2 6 8]))
           :else (first avail))))

     (declare ttt-show-board!)

     (defn- ttt-make-move! [pos player]
       (swap! ttt-state update :board assoc pos player)
       (let [board (:board @ttt-state)
             winner (ttt-check-winner board)]
         (cond
           winner
           (do
             (swap! ttt-state assoc :game-over? true)
             (send-result! "TTT_GAME_OVER" {:winner winner :board board})
             (toast/toast! #js {:autoClose 5000 :position "top-center" 
                               :type (if (= winner "X") "success" "error")}
                          (if (= winner "X") "🏆 YOU WIN!" "😈 AI WINS!")))
           
           (ttt-board-full? board)
           (do
             (swap! ttt-state assoc :game-over? true)
             (send-result! "TTT_GAME_OVER" {:winner "draw" :board board})
             (toast/toast! #js {:autoClose 5000 :position "top-center" :type "info"}
                          "🤝 It's a DRAW!"))
           
           (= player "X")
           ;; AI's turn
           (js/setTimeout
            (fn []
              (let [ai-pos (ttt-ai-move (:board @ttt-state))]
                (ttt-make-move! ai-pos "O")))
            500)
           
           :else
           ;; Show board for player's turn
           (ttt-show-board!))))

     (defn- ttt-show-board! []
       (let [board (:board @ttt-state)
             available (ttt-available board)
             toast-id (atom nil)
             answered? (atom false)
             cell-style {:width "40px" :height "40px" :margin "2px"
                        :fontSize "20px" :fontWeight "bold" :border "1px solid #ccc"}
             make-cell (fn [i]
                        (let [v (get board i)]
                          (if (and (nil? v) (not (:game-over? @ttt-state)))
                            ;; Clickable empty cell
                            (toast/dom-button
                             {:style (merge cell-style {:cursor "pointer" :backgroundColor "#e8f5e9"})
                              :onClick (fn []
                                         (when (compare-and-set! answered? false true)
                                           (.dismiss toast/raw-toast @toast-id)
                                           (ttt-make-move! i "X")))}
                             (str (inc i)))
                            ;; Filled or disabled cell
                            (toast/dom-button
                             {:style (merge cell-style {:backgroundColor (case v "X" "#bbdefb" "O" "#ffcdd2" "#f5f5f5")})
                              :disabled true}
                             (or v " ")))))]
         (reset! toast-id
           (toast/raw-toast
            (fn [_]
              (toast/dom-div {:style {:textAlign "center"}}
                (toast/dom-div {:style {:fontWeight "bold" :marginBottom "8px"}} 
                              "Your turn (X):")
                (toast/dom-div {:style {:display "grid" :gridTemplateColumns "repeat(3, 46px)" :justifyContent "center"}}
                  (make-cell 0) (make-cell 1) (make-cell 2)
                  (make-cell 3) (make-cell 4) (make-cell 5)
                  (make-cell 6) (make-cell 7) (make-cell 8))))
            #js {:autoClose false :closeOnClick false :draggable false :position "top-center"}))))

     (defn start-ttt! []
       (reset! ttt-state {:board [nil nil nil nil nil nil nil nil nil]
                          :game-over? false})
       (send-result! "TTT_GAME_START" {})
       (ttt-show-board!))))
