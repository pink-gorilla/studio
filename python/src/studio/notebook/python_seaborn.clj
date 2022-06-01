(ns studio.notebook.python-seaborn
  (:require
   [libpython-clj2.require :refer [require-python]]
   [libpython-clj2.python :as py :refer [py. py.. py.-]]
   [studio.python.maplot :refer [with-show]]))

(require-python '[seaborn :as sns]) ; seaborn: Really cool statistical plotting
(require-python '[matplotlib.pyplot :as pyplot])

(sns/set) ;;; set default style


;;; code tutorial from https://seaborn.pydata.org/introduction.html

(def dots (sns/load_dataset "dots"))
(py. dots head)

(take 5 dots) 

;; seaborn will be most powerful when your datasets have a particular organization. This format is alternately called “long-form” or “tidy” data and is described in detail by Hadley Wickham in this academic paper. The rules can be simply stated:

;; Each variable is a column

;; Each observation is a row


;;;; statistical relationship plotting
(with-show
  (sns/relplot :x "time" :y "firing_rate" :col "align"
               :hue "choice" :size "coherence" :style "choice"
               :facet_kws {:sharex false} :kind "line"
               :legend "full" :data dots))



;;;; statistical estimateion and error bars

(def fmri (sns/load_dataset "fmri"))

(with-show
  (sns/relplot :x "timepoint" :y "signal" :col "region"
               :hue "event" :style "event" :kind "line"
               :data fmri))



;;; enhance a scatter plot to include a linear regression model

(def tips (sns/load_dataset "tips"))
(with-show
  (sns/lmplot :x "total_bill" :y "tip" :col "time" :hue "smoker" :data tips))



;;; data analysis between caterogical values

(with-show
  (sns/catplot :x "day" :y "total_bill" :hue "smoker" :kind "swarm" :data tips))



(with-show
  (sns/catplot :x "day" :y "total_bill" :hue "smoker" :kind "bar" :data tips))


;;; visualizing dataset structure
(def iris (sns/load_dataset "iris"))
(with-show
  (sns/jointplot :x "sepal_length" :y "petal_length" :data iris))


(with-show
  (sns/pairplot :data iris :hue "species"))

