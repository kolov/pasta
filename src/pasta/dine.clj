(ns pasta.dine
  (:require [clojure.core.async
             :as async
             :refer [chan >! >!! <! <!! timeout alts! alts!!]
             ])
  )

(def N 5)
(def TO 500)

(def forks (repeatedly N (fn[] (let[c (chan 1)] (>!! c :fork) c))))

(defn left[n] (nth forks (mod (+ N (dec n)) N)))
(defn right[n] (nth forks (mod (inc n) N)))

(defn take-fork[c to] (let[t (timeout to)
                           [v c] (alts!! [c t])] v))

(defn put-fork[c] (>>! c :fork))

(defn chew[n period] (let[t (timeout period)] ()))
(defn eat[n]
  (let[l (take-fork (left n) TO)
       r (take-fork (right n) TO)
       ]
    (if l (print "P" n " took left fork"))
    (if r (print "P" n " took right fork"))
    (if (and l r) (print "P" n "  eating"))
    (wait)
    ))

