(ns pinkgorilla.clojisr.repl
  "require this namespace in order to work with clojisr from 
   a pinkgorilla notebook."
  (:require
   [clojisr.v1.require :refer [require-r]]
   [clojisr.v1.r :as r :refer [r]]
   [clojisr.v1.applications.plotting :refer [plot->file]]
   [pinkgorilla.clojisr.util :refer [fix-svg]]
   [pinkgorilla.clojisr.renderer])   ; bring renderer to scope, so notebook user does not need two requires

  (:import [java.io File]))

(defn pdf-off
  "By default R plots are also being rendered to PDF files.
  To disable this behavior, call pdf-off."
  []
  (require-r '[grDevices])
  ;(require-r '[grDevices])
  ;(r.grDevices/dev-off)`
  (r "dev.off('pdf')"))


; https://r-lib.github.io/svglite/articles/fonts.html


(defn ->svg
  "calls a plotting-function and renders the output as svg in gorilla-notebook"
  ([plotting-function-or-object]
   (->svg {} plotting-function-or-object))
  ([wrapper-params plotting-function-or-object] ; & svg-params
   (let [tempfile (File/createTempFile "clojisr_notebook_plot" ".svg")
         path     (.getAbsolutePath tempfile)
         wrapper-params (merge {:width 5 :height 5} wrapper-params)
         {:keys [width height]} wrapper-params
        ; R device params:
        ; https://stat.ethz.ch/R-manual/R-devel/library/grDevices/html/cairo.html
        ; svg dimensions are in inches
         ;dpi 96
         svg-options (merge wrapper-params {:height (:height wrapper-params) ;(/  dpi)
                                            :width (:width wrapper-params)}) ; (/  dpi)
         svg-params (into [] (interleave (keys svg-options) (vals svg-options)))]
     (apply plot->file path plotting-function-or-object svg-params)
     (let [result (slurp path)]
       (.delete tempfile)
       ^:R [:div.clojsrplot
            (fix-svg result width height)]))))

; help returns just file path
; and prints the content to the stdout
; (r "capture.output(tools:::Rd2txt(utils:::.getHelpFile(as.character(help(mean)))))")
(defn r-doc
  "docstring for R functions
   rfunc: string"
  [rfunc]
  (r (str "capture.output(tools:::Rd2txt(utils:::.getHelpFile(as.character(help(" rfunc ")))))")))
