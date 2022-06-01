(ns studio.notebook.ml-kmeans
  (:require
   [scicloj.sklearn-clj.metamorph]
   [scicloj.ml.smile.clustering :as clustering]
   ))

;; todo: get DATA without using python.

;; # 3. use Clojure only pipeline
;;  So no python interop in use
;;  It uses clustering algorithms from JVM library Smile

(def fitted-ctx-2
  (ml/fit
   data
   (mm/std-scale  :all {})
   {:metamorph/id :k-means}
   (scicloj.ml.smile.clustering/cluster
    :k-means
    [3 300]
    :cluster)))

(-> fitted-ctx-2 :k-means  :info)

;; # 4. use declarative Clojure only pipeline
;; same as 3), only using metamorph declarative pipelines



(def decl-pipe
  [[:mm/std-scale :all {}]
   {:metamorph/id :k-means}
   [:scicloj.ml.smile.clustering/cluster
    :k-means
    [3 300]
    :cluster]])

(->> decl-pipe
     ml/->pipeline
     (ml/fit-pipe data)
     :k-means
     :info)


;; # 5. in one threading macro, no variables declared
;; same as 4., but written more compact


(->> [[:mm/std-scale :all {}]
      {:metamorph/id :k-means}
      [:scicloj.ml.smile.clustering/cluster
       :k-means
       [3 300]
       :cluster]]
     ml/->pipeline
     (ml/fit-pipe data)
     :k-means
     :info)