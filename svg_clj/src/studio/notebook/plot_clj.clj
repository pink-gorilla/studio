(ns studio.notebook.plot-clj
  (:require
   [cljplot.core :as cljplotl]
   [cljplot.render :as r]
   [cljplot.build :as b]
   [cljplot.common :as cpc]
   [reval.ui :refer [img]]
   [reval.document.manager :as rdm]))

(defn vega-clj [data]
  (-> (b/series
       [:grid nil {:x nil}]
       [:stack-vertical [:bar data {:padding-out 0.1}]])
      (b/preprocess-series)
      (b/update-scale :x :fmt name)
      (b/add-axes :bottom)
      (b/add-axes :left)
      (b/add-label :bottom "a")
      (b/add-label :left "b")
      (r/render-lattice {:width 400 :height 400})
      ;(save "bar.jpg")
      ;(show)
      :buffer))

(defn vega-clj-img [data]
  (img (vega-clj data)))

;; generate plot-image, and show it in browser

(-> {:A 10 :B 55 :C 43 :D 91 :E 81 :F 53 :G 19 :H 87 :I 52}
         vega-clj-img)

(-> {:A -10 :B 55 :C 43 :D 91 :E 81 :F 53 :G 19 :H 87 :I 52}
         vega-clj-img)

(-> {:A -20 :B 55 :C 43 :D 91 :E 81 :F 53 :G 19 :H 87 :I 52}
 vega-clj-img)
  
; vega-clj works with sorted map too
(def data2 (sorted-map :A 28 :B 55 :C 43 :D 91 :E 81 :F 53 :G 19 :H 87 :I 52))
(vega-clj-img data2)

; save plot 
(-> {:A 28 :B 55 :C 43 :D 91 :E 81 :F 53 :G 19 :H 87 :I 52}
     vega-clj
    (rdm/save *ns* "item-plot" :png))

; show url
(rdm/get-link-ns *ns* "item-plot.png")

; test - can we determine the format?
;(rdm/filename->extension "item-plot.png")
; (rdm/filename->format "item-plot.png")
