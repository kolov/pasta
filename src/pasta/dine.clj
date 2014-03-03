(ns pasta.dine
  (:require [clojure.core.async
             :as async
             :refer [chan >! >!! <! <!! timeout alts! alts!! thread go]
             ])
  )



;; simpple thread-safe logging mecahnism, order not guaranteed
(def log-channel (chan 1000))
(defn log [& msgs] (go (>! log-channel (apply str msgs))))
(thread (while true (println (<!! log-channel))))

(def N "Number of philosophers" 5)
(defn philosopher [n] (str "Philosopher " n))


(def forks (repeatedly N (fn [] (let [c (chan 1)] (>!! c :fork) c))))

(defn left-fork [n] (nth forks (mod (+ N (dec n)) N)))
(defn right-fork [n] (nth forks (mod (inc n) N)))
(defn fork-channel [n side]
  (case side
    :left (left-fork n)
    :right (right-fork n)))


(defn now [] (System/currentTimeMillis))
(defn wait [t] (<!! (timeout t)))

(defn grab-fork [n side t out]
  "Starts a thread trying to grab a fork. On success, passes the fork to channel out"
  (go (do
        (log (philosopher n) " waits for " side " fork")
        (let [[fork _] (alts! [(fork-channel n side) (timeout t)])]
          (when fork
            (log (philosopher n) " takes " side " fork")
            (>! out :left))))))

(defn return-fork [n side channel]
  (log (philosopher n) " puts " side " fork") (>!! channel :fork))

(defn try-to-eat [n t]
  "Get both forks with timeout"
  (let [deadline (+ t (now))
        c-both (chan 2)]
    (grab-fork n :left t c-both)
    (grab-fork n :right t c-both)
    (let [
           [f1 c1] (alts!! [c-both (timeout t)])
           time-left (max 0 (- deadline (now)))
           [f2 c2] (alts!! [c-both (timeout time-left)])]
      (if (and f1 f1)
        (do (log (philosopher n) " eating") (wait (rand-int 3000)))
        (log (philosopher n) " coud not eat with forks " (if f1 f1 :none) ", " (if f2 f2 :none)))
      (when f1 (return-fork n f1 c1))
      (when f2 (return-fork n f2 c2))
      )
    ))

(def party-on (atom true))
(defn set-party-on [v] (swap! party-on (fn [_] v)))
(defn dine-once []
  (doseq [n (range N)] (thread (do (wait (rand-int 2000)) (try-to-eat n (rand-int 5000))))))
(defn dine []
  (set-party-on true)
  (doseq [n (range N)]
    (thread (while @party-on (do
                               (wait (rand-int 2000))
                               (try-to-eat n (rand-int 5000))))
            (log "Party OVER for " (philosopher n)))))

