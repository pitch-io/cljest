(ns cljest.timers
  (:require-macros [cljest.timers])
  (:require ["@jest/globals" :refer [jest]]
            [cljest.core :refer [after-each before-each]]
            [cljest.helpers.dom :as h.dom]))

(defn use-fake-timers!
  "Uses fake timers within this block of tests, essentially calling `jest.useFakeTimers`
  before each test and `useRealTimers` after each test.

  Must be run on the top or describe level. For test level fake timers,
  use `cljest.timers/with-fake-timers`."
  ([] (use-fake-timers! js/undefined))
  ([options]
   (before-each
     (.useFakeTimers jest options)
     (h.dom/-use-fake-timers-event-context!))
   (after-each
     (.runOnlyPendingTimers jest)
     (h.dom/-use-default-event-context!)
     (.useRealTimers jest))))

(defn run-all-ticks
  "Exhaust the microtask queue (runs all pending tasks queued via process.nextTick)."
  []
  (.runAllTicks jest))

(defn run-all-timers
  "Exhaust the macrotask and microtask queues."
  []
  (.runAllTimers jest))

(defn run-all-timers+
  "Async version of run-all-timers. Runs any schedules promises _before_ timers."
  []
  (.runAllTimersAsync jest))

(defn advance-timers-by-time
  "Advances timers by the provides `ms` value. Will run any macrotasks that are currently
  or get queued within that time frame."
  [ms]
  (.advanceTimersByTime jest ms))

(defn advance-timers-by-time+
  "Async version of advance-timers-by-time. Runs scheduled promises _before_ macrotasks."
  [ms]
  (.advanceTimersByTimeAsync jest ms))

(defn run-only-pending-timers
  "Executes only the currently pending macrotasks. Does not exhaust and any newly queued macrotasks
  won't get executed."
  []
  (.runOnlyPendingTimers jest))

(defn run-only-pending-timers+
  "Async version of `run-only-pending-timers`. Runs scheduled promise callbacks _before_ executing pending
  macrotasks."
  []
  (.runOnlyPendingTimersAsync jest))

(defn advance-timers-to-next-timer
  "Advances all timers by the needed milliseconds so that only the next timeouts/intervals will run.

  If given, will run `steps` amount of next timeouts/intervals."
  ([] (advance-timers-to-next-timer js/undefined))
  ([steps] (.advanceTimersToNextTimer jest steps)))

(defn advance-timers-to-next-timer+
  "Async version of `advance-timers-to-next-timer`. Runs scheduled promises _before_ timers."
  ([] (advance-timers-to-next-timer+ js/undefined))
  ([steps] (.advanceTimersToNextTimerAsync jest steps)))

(defn clear-all-timers
  "Removes all pending timers."
  []
  (.clearAllTimers jest))

(defn get-timer-count
  "Gets the count of queued timers."
  []
  (.getTimerCount jest))

(defn now
  "Returns current clock time, in ms. Equivalent to `(js/Date.now)` if using real timers or if `Date` is mocked."
  []
  (.now jest))

(defn set-system-time
  "Sets the current system time used by the fake timers. Will _not_ run any pending timers, even if changing the time
  to the future would theoretically cause them to run."
  ([] (set-system-time js/undefined))
  ([now] (.setSystemTime jest now)))

(defn get-real-system-time
  "Gets the actual system time, even if the timers are currently faked."
  []
  (.getRealSystemTime jest))
