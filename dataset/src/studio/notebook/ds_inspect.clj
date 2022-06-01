(ns studio.notebook.ds-inspect
  (:require
   [tablecloth.api :as tc]
   [studio.dataset.inspect :refer [has-col]]))


(def d 
  (tc/dataset {:close [1 2 3]
               :adj-close [5 7 8]}))


(has-col d :x)

(has-col d :close)
