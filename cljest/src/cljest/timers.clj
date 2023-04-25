(ns cljest.timers
  (:import [cljs.tagged_literals JSValue]))

(defmacro with-fake-timers
  "Runs the provided forms with fake timers, resetting to using real timers at the
  end of the forms execution. Handles promises.

  You can optionally provide options to the fake timers setup, and these options
  must be a JS value (you need to use the #js reader)."
  [maybe-options & other-forms]
  (let [options-provided? (instance? JSValue maybe-options)
        options (if options-provided? maybe-options 'js/undefined)
        forms (if options-provided? other-forms (into [maybe-options] other-forms))]
    `(-> (js/Promise.resolve)
         (.then (fn []
                  (cljest.helpers.dom/-use-fake-timers-event-context!)
                  (js/jest.useFakeTimers ~options)))
         (.then (fn [] ~@forms))
         (.then (fn []
                  (cljest.helpers.dom/-use-default-event-context!)
                  (js/jest.useRealTimers))))))
