(ns studio.notebook.ds-vega
  (:require
   [tech.v3.dataset :as tds]
   [tech.v3.datatype.datetime.operations :as dtype-dt-ops]
   [tech.viz.vega :as vega]
   [studio.dataset.viz :refer [show-vega]]
   ))

(defn stock-plot []
  (as-> (tds/->dataset "https://vega.github.io/vega/data/stocks.csv") ds
      ;;The time series chart expects time in epoch milliseconds
  ;(tds/add-or-update-column ds "year" (dtype-dt-ops/get-years (ds "date")))
  ;(tds/filter-column #{2007 2008 2009} "year" ds)
    (tds/update-column ds "date" #(dtype-dt-ops/datetime->epoch :epoch-milliseconds %))
    (tds/mapseq-reader ds)
  ;;all graphing functions run from pure clojure data.  No batteries required.
    (vega/time-series ds "date" "price"
                      {:title "Stock Price (2007-2010)"
                       :label-key "symbol"
                       :background "white"})

  ;(vega/vega->svg-file ds "timeseries.svg")
    ))
;; generate plot-image, and show it in browser

(-> (stock-plot)
    (show-vega))

