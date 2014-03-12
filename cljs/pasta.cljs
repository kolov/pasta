(ns pasta.dine
  (:require [cljs.core.async
             :refer [chan >! <! timeout put! take! alts!]
             ])
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  )



(defn now [] (.getTime (js/Date.)))

;; basic DOM stuff
(defn append [txt id]
  (let [el (.getElementById js/document id)]
    (set! (.-innerHTML el) (str (.-innerHTML el) txt))))
(defn clear [id] (set! (.-innerHTML (.getElementById js/document id)) ""))

;; simple thread-safe logging mecahnism, order not guaranteed.
(def log-channel (chan 1000))
(defn log [& msgs] (go (>! log-channel (apply str msgs))))
(def begin (atom (now)))


(defn start-logging []
  (reset! begin (now))
  (go (while true
        (let [timestamp (str (- (now) @begin))
              txt (str "<p>" timestamp ": " (<! log-channel) "</p>")]
          (append txt "result")))))

(def N "Number of philosophers" 5)
(defn philosopher [n] (str "Philosopher " n))
(def EAT-TIME 10000)
(def THINK-TIME 10000)
(def TRY-TIME 3000)


(def forks (repeatedly N (fn [] (let [c (chan 1)] (go (>! c :fork)) c))))
(def philos (repeatedly N (fn [] (let [c (chan 1)] (go (>! c :ready)) c))))

(defn left-fork [n] (nth forks n))
(defn right-fork [n] (nth forks (mod (inc n) N)))

(defn fork-channel [n side]
  (case side
    :left (left-fork n)
    :right (right-fork n)))

(defn philo-channel [n] (nth philos n))

(defn grab-fork [n side out]
  "Starts a go block trying to grab a fork. Passes the fork to channel out"

  (go
    (log (philosopher n) " waits for " side " fork")
    (let [[fork _] (alts! [(fork-channel n side) (timeout (rand-int TRY-TIME))])]
      (if fork
        (do (log (philosopher n) " takes " side " fork") (>! out side))
        (do (log (philosopher n) " did not take " side " fork") (>! out :none))))))

(defn return-fork [n side]
  (log (philosopher n) " puts " side " fork")
  (put! (fork-channel n side) :fork))

(defn eat [n out]
  (go
    (let [f1 (<! out)
          f2 (<! out)
          got (set (filter #(not= % :none) [f1 f2]))]
      (if (= got #{:left, :right})
        (do (log (philosopher n) " *** eating *** ") (<! (timeout (rand-int EAT-TIME))))
        (log (philosopher n) " coud not eat with forks " got))

      (doseq [side got] (return-fork n side))
      (<! (timeout (rand-int THINK-TIME)))
      (>! (philo-channel n) :ready)
      )))

(def dinner-on? (atom true))
(defn finish-dinner [] (reset! dinner-on? false))
(defn start-dinner [] (reset! dinner-on? true))

(defn party [n out]
  (go
    (while @dinner-on?
      (do
        (<! (philo-channel n))
        (grab-fork n :left out)
        (grab-fork n :right out)
        (eat n out)
        )
      )
    (log "Party OVER for " (philosopher n))
    ))

(defn dine []
  (clear "result")
  (log "Dinner served")
  ;(start-dinner)
  (start-logging)
  (doseq [n (range N)]
    (let [out (chan 2)]
      (log "Enters " (philosopher n))
      (party n out)
      )))


; make visible in javascript
(defn ^:export startDinner [] (dine))
(defn ^:export stopDinner [] (finish-dinner))

