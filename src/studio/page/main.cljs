(ns studio.page.main
  (:require
    ;[user :refer [link-href]]
   [studio.lib.ui :refer [add-page link-href]]))

; main page 

(defn main-page  [{:keys [handler route-params query-params] :as route}]
  [:div
   [:h1.text-2xl.text-red-600.m-5 "Goldly Studio"]
   [:p "This website shows what you can do with goldly."]])

(add-page main-page :studio/main)
