(ns studio.lib.ui
  (:require
   [user :as user]
   [site]
   [layout]))

;; links

(defn link-fn [fun text]
  [:a.bg-blue-600.cursor-pointer.hover:bg-red-700.m-1
   {:on-click fun} text])

(defn link-dispatch [rf-evt text]
  (link-fn #(rf/dispatch rf-evt) text))

(defn link-href [href text]
  [:a.bg-blue-600.cursor-pointer.hover:bg-red-700.m-1
   {:href href} text])

;; site layout

(defn site-header []
  [site/header-menu
   {:brand "GoldlyStudio"
    :brand-link "/"
    :items [{:text "notebook viewer" :dispatch [:bidi/goto :viewer :query-params {}]}
            {:text "repl"  :dispatch [:bidi/goto :repl]}
            {:text "dev-tools"  :dispatch [:bidi/goto :devtools]}
            {:text "feedback" :link "https://github.com/pink-gorilla/studio/issues" :special? true}]}])

(defn add-page [page name]
  (let [wrapped-page (fn [route]
                       [layout/header-main  ; .w-screen.h-screen
                        [site-header]
                        [page route]])]
    (user/add-page wrapped-page name)))



