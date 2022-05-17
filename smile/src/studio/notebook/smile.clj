(ns studio.notebook.smile
  (:require
   [taoensso.timbre :refer [trace debug info infof  error]]
   [smile.clustering :refer [kmeans]]))


(let [k 2
      data  [0 6 5 4 3]
            ;[0 8 4 3 2]
            ;[6 5 3 2 1] ]
      result (kmeans data k)]
    (info "clusters: " result)
    result
  )