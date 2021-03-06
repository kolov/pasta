(ns pasta.dine
  (:require [clojure.core.async
             :as async
             :refer [chan >! >!! <! <!! timeout alts! alts!! go]
             ])
  )

(defn now [] (System/currentTimeMillis))

;; simple thread-safe logging mecahnism, order not guaranteed
(def log-channel (chan 1000))
(defn log [& msgs] (>!! log-channel (apply str msgs)))
(def begin (atom (now)))
;; print whatever cen be read from log-channel
(go (while true (println
                  (format "%05d" (- (now) @begin))
                  (<! log-channel))))

(def N "Number of philosophers" 5)
(defn philosopher [n] (str "Philosopher " n))


(def forks
  "vector of N channels - one for each fork"
  (repeatedly N (fn [] (let [c (chan 1)] (>!! c :fork) c))))

(defn left-fork [n] (nth forks n))
(defn right-fork [n] (nth forks (mod (inc n) N)))

(defn fork-channel [n side]
  (case side
    :left (left-fork n)
    :right (right-fork n)))

(defn grab-fork [n side t out]
  "Starts a go-block trying to grab a fork. Passes the :fork or :none (on timeout) to channel out"
  (go (do
        (log (philosopher n) " waits for " side " fork")
        (let [[fork _] (alts! [(fork-channel n side) (timeout t)])]
          (if fork
            (do (log (philosopher n) " takes " side " fork") (>! out side))
            (do (log (philosopher n) " did not take " side " fork") (>! out :none)))))))

(defn return-fork [n side]
  "Puts a fork back in the channel"
  (log (philosopher n) " puts " side " fork")
  (go (>! (fork-channel n side) :fork)))

(defn try-to-eat [n t]
  "Try to get both forks with timeout. On success, eat for a while. Put any obtained forks back."
  (let [c-both (chan 2)]
    ; try to grab both forks. Some combination of 2 objects :fork or :none will go to c-both channel
    (grab-fork n :left t c-both)
    (grab-fork n :right t c-both)
    (let [
           f1 (<!! c-both)
           f2 (<!! c-both)
           got (set (filter #(not= % :none) [f1 f2]))
           ]
      (if (= got #{:left, :right})
        (do (log (philosopher n) " *** eating *** ") (<!! (timeout (rand-int 3000))))
        (log (philosopher n) " coud not eat with forks " got))
      (doseq [side got] (return-fork n side))
      )
    ))

(def dinner-on? (atom true))
(defn finish-dinner [] (reset! dinner-on? false))
(defn start-dinner [] (reset! dinner-on? true) (reset! begin (now)))

(defn dine []
  (start-dinner)
  (doseq [n (range N)]
    (go (do
          (while @dinner-on?
            (do
              ; wait 0-2sec...
              (<! (timeout (rand-int 2000)))
              ; then try to get forks with timeout of 0-5sec.
              (try-to-eat n (rand-int 5000))))
          (log "Party OVER for " (philosopher n))))))

