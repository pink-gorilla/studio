(ns notebook.studio.panthera-objects
  (:require
   [libpython-clj.python :as py]
   [libpython-clj.require :refer [require-python]]
   [panthera.panthera :as pt]
   [pinkgorilla.python.core :refer [py-initialize!]]
   [pinkgorilla.python.plot :refer [with-show]]))

;;; # Introducing **panthera** data structures

(py-initialize!)

(require-python '[numpy :as np])

(def data (pt/series [0.25 0.5 0.75 1.0]))
data



(pt/values data)
(pt/pytype (pt/values data))
(pt/index data)

(pt/select-rows data 1)
(pt/subset-rows data 1 3)

(def data (pt/series [0.25 0.5 0.75 1.0]
                     {:index [:a :b :c :d]}))
data


(pt/select-rows data :b :loc)

(def pop-map
  {:California 38332521
   :Texas 26448193
   "New York" 19651127
   :Florida 19552860
   :Illinois 12882135})

(def population (pt/series pop-map))
population



(pt/select-rows population :California :loc)
(pt/select-rows population (pt/slice :California :Illinois) :loc)

(pt/series 5 {:index [100 200 300]})



(def area-map
  {:California 423967
   :Texas 695662
   "New York" 141297
   :Florida 170312
   :Illinois 149995})

(def area (pt/series area-map))
area


(def states (pt/data-frame {:population population
                            :area area}))

states

(pt/index states)


(pt/names states)
(pt/subset-cols states :area)
(pt/data-frame population {:columns [:population]})

(pt/data-frame [{:a 1 :b 2} {:b 3 :c 4}])

(pt/data-frame (py/$a np/random rand 3 2)
               {:columns [:foo :bar]
                :index [:a :b :c]})
