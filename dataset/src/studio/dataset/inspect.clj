(ns studio.dataset.inspect
  (:require
   [tech.v3.dataset :as tds]
   [tech.v3.dataset.print :refer [print-range]]
   [tablecloth.api :as tc]))

(defn ds->map [ds]
  (into [] (tds/mapseq-reader ds)))

(defn ds->str [ds]
  (let [ds-full (print-range ds :all)
        t (with-out-str
            (println ds-full))]
    t))

(defn show-meta [ds]
  (->> ds tc/columns (map meta) (map (juxt :name :datatype))))

(defn cols-of-type [ds t]
  (->> ds
       tc/columns
       (map meta)
       (filter #(= t (:datatype %)))
       (map :name)))

(defn drop-instant-cols [ds]
  (tc/drop-columns ds #(= :packed-instant %) :datatype))

(defn has-col [ds col]
  (->> ds
       tc/columns
       (map meta)
       (filter #(= col (:name %)))
       empty?
       not
       ;(map :name)
       ))
