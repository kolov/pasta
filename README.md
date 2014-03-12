# pasta

Dining philosophers with Clojure async

The [Dining Philosophers Problem](http://en.wikipedia.org/wiki/Dining_philosophers_problem) is an example problem
illustrating the challenges about synchronizing access to limited shared resources.

This little project is an application of Clojure core.async to this problem. Each philosopher repeats these steps:
 1. Try to get left and right fork *simultaneously*
 2. After a given timeout,
   a) if in possession of both forks, eat for a while and return  forks
   b) otherwise, return whatever forks obtained immediately
 3. Think for a while

The solution I came with in ClojureScript required several channels:
 - for each fork, a channel with capacity 1 initialized with a fork. Only one process can get the fork.
 - for each philosopher, a channel with capacity 1, indicating if the philosopher is trying to eat
 - for each eating attempt, a channel with capacity 2, containing obtained forks, or :none.

The beauty of core.async is that it allows writing async code for the browser, as though javascript wasn't single-threaded.
See it running at http://code.akolov.com/pasta/dine.html
