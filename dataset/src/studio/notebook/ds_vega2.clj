(ns studio.notebook.ds-vega2
  (:require
   [tech.ml.dataset :as ds]
   [tech.v2.datatype.datetime.operations :as dtype-dt-ops]
   [tech.viz.vega :as vega]))

;;; # tech.ml.dataset - vega plots
;;; https://github.com/techascent/tech.ml.dataset/blob/master/CHANGELOG.mds


^:R ['user/vega "https://raw.githubusercontent.com/vega/vega/master/docs/examples/bar-chart.vg.json"]

;; **
;;; # Scatterplot
;; **


(vega/scatterplot [{:a 1 :b 2} {:a 2 :b 3}] :a :b)


;; **
;;; # Timeseries plots
;; **


(def location "https://fred.stlouisfed.org/graph/fredgraph.csv?bgcolor=%23e1e9f0&chart_type=line&drp=0&fo=open%20sans&graph_bgcolor=%23ffffff&height=450&mode=fred&recession_bars=on&txtcolor=%23444444&ts=12&tts=12&width=1168&nt=0&thu=0&trc=0&show_legend=yes&show_axis_titles=yes&show_tooltip=yes&id=UEMPMED&scale=left&cosd=1967-07-01&coed=2020-03-01&line_color=%234572a7&link_values=false&line_style=solid&mark_type=none&mw=3&lw=2&ost=-99999&oet=99999&mma=0&fml=a&fq=Monthly&fam=avg&fgst=lin&fgsnd=2009-06-01&line_index=1&transformation=lin&vintage_date=2020-04-17&revision_date=2020-04-17&nd=1967-07-01")

(def ds* (delay (ds/->dataset location)))


(defn graph-ds
  []
  (-> @ds*
      (ds/update-column "DATE" dtype-dt-ops/get-epoch-milliseconds)
      (ds/interpolate-loess "DATE" "UEMPMED" {:bandwidth 0.01
                                              :iterations 2
                                              :result-name "UEMPMED-loess"})
      (ds/column-labeled-mapseq ["UEMPMED" "UEMPMED-loess"])
      (vega/time-series "DATE" :value {:label-key :label
                                       :background "white"})))



(graph-ds)

(ds/select (ds/->dataset "https://vega.github.io/vega/data/stocks.csv")
           :all (range 5))

(as-> (ds/->dataset "https://vega.github.io/vega/data/stocks.csv") ds
      ;;The time series chart expects time in epoch milliseconds
  (ds/add-or-update-column ds "year" (dtype-dt-ops/get-years (ds "date")))
  (ds/filter-column #{2007 2008 2009} "year" ds)
  (ds/update-column ds "date" dtype-dt-ops/get-epoch-milliseconds)
  (ds/mapseq-reader ds)
	  ;;all graphing functions run from pure clojure data.  No batteries required.
  (vega/time-series ds "date" "price"
                    {:title "Stock Price (2007-2010)"
                     :label-key "symbol"
                     :background "white"}))