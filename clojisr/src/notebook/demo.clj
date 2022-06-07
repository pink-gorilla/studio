
  
(ns pinkgorilla.clojisr.demo
  (:require
   ;[tech.ml.dataset :as dataset]
   ;[clojisr.v1.r :as r :refer [r r->clj clj->r r+ colon bra bra<- rdiv r** r- r* ->code]]
   [clojisr.v1.require :refer [require-r]]
   [clojisr.v1.applications.plotting :refer [plot->svg #_plot->file]]
   [pinkgorilla.clojisr.repl :refer [->svg  pdf-off]]))

(println "configuring clojisr ..")
(require-r '[base :as base :refer [$ <- $<-]]
           '[utils :as u]
           '[stats :as stats]
           '[graphics :as g :refer [plot hist]]
           '[datasets :refer :all]
           '[ggplot2 :refer [ggplot aes geom_point xlab ylab labs]])

(base/options :width 120 :digits 7)
(base/set-seed 11228899)
(pdf-off)
(println "clojisr configuring finished!")

(defn hist-plot []
  (println "svg: "
           (->svg {:width 384 :height 480}
                  (fn [] ; without this it does not work.
                    (hist [1 1 1 1 2 3 4 5]
                          :main "Histogram"
                          :xlab "data: [1 1 1 1 2 3 4 5]")))))

(hist-plot)

(def target-path "./")

(defn hist-plot-file []
  ;(println
   ;"r plot saved: "
   ;(r->clj
  (plot->svg ; plot->file
     ;(str target-path "histogram.jpg")
   (fn []  ; without this it does not work.
     (hist [1 1 1 1 2 3 4 5]
           :main "Histogram"
           :xlab "data: [1 1 1 1 2 3 4 5]"))
   :onefile true
   :width 8
   :height 4
     ;:quality 50
   ))

(hist-plot-file)