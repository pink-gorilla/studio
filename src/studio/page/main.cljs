(ns studio.page.main
  (:require
   [studio.lib.ui :refer [add-page-themed]]))

; main page 

(defn main-page  [_route-data]
  [:div
   [:h1.text-2xl.text-red-600.m-5 "Goldly Studio"]
   [:p "This website shows what you can do with goldly."]])

(add-page-themed main-page :studio/main)
