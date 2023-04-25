(ns cljest.example-test
  (:require [cljest.core :refer [before-each describe is it]]
            [cljest.helpers.core :as h]
            [cljest.helpers.dom :as h.dom]
            [cljest.matchers :as m]
            [cljest.timers :as t]
            [uix.core :refer [$ defui] :as uix]))

(defui the-bakery
  []
  (let [[count set-count!] (uix/use-state 0)
        timeout-ids (uix/use-ref [])
        handle-click (fn []
                       (set-count! inc)

                       (let [timeout-id (js/setTimeout #(set-count! inc) 1000)]
                         (swap! timeout-ids conj timeout-id)))]

    (uix/use-effect
     (fn []
       (fn []
         (doseq [timeout-id @timeout-ids]
           (js/clearTimeout timeout-id))))
     [])

    ($ :div
       ($ :h1 (str count " cookies"))
       ($ :button {:on-click handle-click} "Bake some cookies"))))

(describe "the-bakery"
  (before-each (h.dom/render ($ the-bakery)))

  (it "should increment the count when the button is clicked"
    (h/async
     (is (m/visible? (h.dom/get-by :text "0 cookies")))
     (await (h.dom/click+ (h.dom/get-by :text "Bake some cookies")))
     (is (m/visible? (h.dom/get-by :text "1 cookies")))))

  (it "should add a new cookie every second after the button is clicked"
    (t/with-fake-timers
      (h/async
       (await (h.dom/click+ (h.dom/get-by :text "Bake some cookies")))
       (is (m/visible? (h.dom/get-by :text "1 cookies")))

       (t/advance-timers-by-time 1000)
       (await (h.dom/wait-for+ #(is (m/visible? (h.dom/get-by :text "2 cookies")))))))))
