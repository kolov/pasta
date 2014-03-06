(ns pasta.dine
  (:require [cljs.core.async
             :refer [chan >! <! timeout]
             ])
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  )



(defn now [] (.getTime (js/Date.)))

;; simple thread-safe logging mecahnism, order not guaranteed.
(def log-channel (chan 1000))
(defn log [& msgs] (go (>! log-channel (apply str msgs))))
(def begin (atom (now)))

(defn append [txt id]
  (let [el (.getElementById js/document "result")]
    (set! (.-innerHTML el) (str (.-innerHTML el) txt))))

(defn start-logging []
  (go (while true
        (let [timestamp (str (- (now) @begin))
              txt (str "<p>" timestamp ": " (<! log-channel) "</p>")]
          (append txt "result")))))

(def N "Number of philosophers" 5)
(defn philosopher [n] (str "Philosopher " n))
(def EAT-TIME 10000)
(def REST-TIME 10000)
(def TRY-TIME 3000)


(def forks (repeatedly N (fn [] (let [c (chan 1)] (go (>! c :fork) c)))))

(defn left-fork [n] (nth forks n))
(defn right-fork [n] (nth forks (mod (inc n) N)))

(defn fork-channel [n side]
  (case side
    :left (left-fork n)
    :right (right-fork n)))




(defn grab-fork [n side t out]
  "Starts a thread trying to grab a fork. Passes the fork to channel out"
  (go (do
        (log (philosopher n) " waits for " side " fork")
        (let [[fork _] (alts! [(fork-channel n side) (timeout t)])]
          (if fork
            (do (log (philosopher n) " takes " side " fork") (>! out side))
            (do (log (philosopher n) " did not take " side " fork") (>! out :none)))))))

(defn return-fork [n side]
  (log (philosopher n) " puts " side " fork")
  (go (>! (fork-channel n side) :fork)))

(defn try-to-eat [n t]
  "Get both forks with timeout"
  (let [deadline (+ t (now))
        c-both (chan 2)]
    (grab-fork n :left t c-both)
    (grab-fork n :right t c-both)
    (go
      (let [
             f1 (<! c-both)
             f2 (<! c-both)
             got (set (filter #(not= % :none) [f1 f2]))
             ]
        (if (= got #{:left, :right})
          (do (log (philosopher n) " *** eating *** ") (<! (timeout (rand-int EAT-TIME))))
          (log (philosopher n) " coud not eat with forks " got))

        (map #(return-fork n %) got)
        ))
    ))

(def dinner-on? (atom true))
(defn set-dinner-on [v] (swap! dinner-on? (constantly v)))
(defn finish-dinner [] (set-dinner-on false))
(defn start-dinner [] (set-dinner-on true))


(defn dine []
  (set-dinner-on true)
  (swap! begin (constantly (now)))
  (doseq [n (range N)]
    (go (do
          (while @dinner-on?
            (do
              (try-to-eat n (rand-int TRY-TIME))
              (<! (timeout (rand-int REST-TIME)))
              ))
          (log "Party OVER for " (philosopher n))))))

(start-logging)

; make visible in javascript
(defn  ^:export startDinner [] (dine))
(defn  ^:export stopDinner [] (finish-dinner))
