(ns pinkgorilla.dsl.r.ggplot
  (:import (java.io File)
           (java.util UUID))
  (:require
   [clojure.java.shell :as shell]
   [clojure.string :as string]
   [clojure.walk :refer [prewalk]]
   [pl.danieljanus.tagsoup :as ts]
   [com.rpl.specter :refer [transform ALL]]
   [pinkgorilla.dsl.r.style :refer [convert-style-as-strings-to-map]]))


(def keep-tempfiles? false)


;; * Functions for building R code *

(declare to-r)

(defn- quote-string
  "Wraps a string in escaped quotes."
  [st]
  (str "\"" st "\""))

(defn- function-name
  "R operators can be called in prefix form with a function name that is the quoted string
  of the operator name. This function handles a selection of the operators as special cases."
  [f]
  (case f
    :+ (quote-string "+")
    :<- (quote-string "<-")
    (name f)))

(defn- fn-from-vec
  "An R function call is represented by a Clojure vector with the function name, given as a keyword, in
  the first element. Subsequent elements can be used to represent positional or named arguments (see below).
  This function transforms one of these function-call vectors into the equivalent R code, returned as a string."
  [vec]
  (str (function-name (first vec)) "("
       (string/join ", " (map to-r (rest vec)))
       ")"))

(defn- named-args-from-map
  "Named arguments to R functions are specified as Clojure maps. This function constructs the snippet of
  the argument string corresponding to the given named arguments. Note that the argument order may not be
  the same as specified when the map is created."
  [arg-map]
  (string/join ", " (map #(str (name %) " = " (to-r (% arg-map))) (keys arg-map))))

(defn r+
  "A helper function for adding things together (i.e. ggplot2 layers). Call it with the arguments you want
  to add together, in the same manner as core/+."
  [& args]
  (reduce (fn [a b] [:+ a b]) args))

(defn to-r
  "Takes a Clojure representation of R code, and returns the corresponding R code as a string."
  [code]
  (cond
    ;; vectors are either function calls or lists of commands
    (vector? code)   (if (vector? (first code))
                       (string/join ";\n" (map to-r code))
                       (fn-from-vec code))
    (map? code)      (named-args-from-map code)
    (keyword? code)  (name code)
    (string? code)   (quote-string code)
    true             (pr-str code)))

(defn data-frame
  "A helper function that takes frame-like data in the 'natural' Clojure format of
  {:key [vector of values] :key2 [vector ...] ...} and returns the Clojure representation
  of R code to make a data.frame."
  [data-map]
  [:data.frame
   (apply hash-map (mapcat (fn [e] [(key e) (into [:c] (val e))]) data-map))])


;; * Functions for driving R *

(defn- rscript
  "Execute a file of R code in a new R session. No output will be returned. If the R process exits abnormally, then the
  error output will be printed to the console."
  [script-path]
  (let [return-val (shell/sh "Rscript" "--vanilla" script-path)]
    ;; rscript is quite chatty, so only pass on err text if exit was abnormal
    (when (not= 0 (:exit return-val))
      (println (:err return-val)))))


;; * Wrappers for ggplot2 functions *

(defn- wrap-ggplot
  "Wraps the given R command with commands to load ggplot2 and save the last plot to the given file."
  [command filepath width height]
  (to-r
   [[:library :ggplot2]
    command
    [:ggsave {:filename filepath :width width :height height}]]))

(defn- mangle-ids
  "ggplot produces SVGs with elements that have id attributes. These ids are unique within each plot, but are
  generated in such a way that they clash when there's more than one plot in a document. This function takes
  an SVG string and replaces the ids with globally unique ids. It returns a string.
  This is a workaround which could be removed if there was a way to generate better SVG in R. Also:
  http://stackoverflow.com/questions/1732348/regex-match-open-tags-except-xhtml-self-contained-tags/1732454#1732454"
  [svg]
  (let [ids (map last (re-seq #"id=\"([^\"]*)\"" svg))
        id-map (zipmap ids (repeatedly (count ids) #(str (UUID/randomUUID))))]
    (-> svg
        (string/replace #"id=\"([^\"]*)\"" #(str "id=\"" (get id-map (last %)) "\""))
        (string/replace #"\"#([^\"]*)\"" #(str "\"#" (get id-map (last %)) "\""))
        (string/replace #"url\(#([^\"]*)\)" #(str "url(#" (get id-map (last %)) ")")))))

(defn render
  "Takes a ggplot2 command, expressed in the Clojure representation of R code, and returns the plot rendered to SVG
  as a string. Options can be passed in a second argument, if wished, as a map. Supported options are :width of plot
  (in inches!) and :height. If only :width is given then a sensible default height will be chosen."
  ([plot-command] (render plot-command {}))
  ([plot-command options]
   (let [width (or (:width options) 6.5)
         height (or (:height options) (/ width 1.618))
         r-file (File/createTempFile "gg4clj" ".r")
         r-path (.getAbsolutePath r-file)
         ;;_ (println r-path)
         svg-file (File/createTempFile "gg4clj" ".svg")
         svg-path (.getAbsolutePath svg-file)
         _ (spit r-path (wrap-ggplot plot-command svg-path width height))
         _ (rscript r-path)
         ;rendered-plot (slurp svg-path)
         rendered-plot (ts/parse-string (slurp svg-path))
         _ (when (not keep-tempfiles?) (.delete r-file))
         _ (when (not keep-tempfiles?) (.delete svg-file))]
     rendered-plot)))


(defn is-tag? [tag x]
  ;(println "is-style? " x)
  (if (and (vector? x)
           (> 1 (count x))
           (= (first x) tag))
    true
    false))

(defn replace-with [tag x]
  (println "replacing tag" tag x)
  (into [] (assoc x 0 tag)))

(defn fix-case-tags
  "resolve function-as symbol to function references in the reagent-hickup-map.
   Leaves regular hiccup data unchanged."
  [svg]
  (prewalk
   (fn [x]
     (cond (is-tag? :viewbox x) (replace-with :viewBox x)
           (is-tag? :textlength x) (replace-with :textLength x)
           (is-tag? :lengthAdjust x) (replace-with :lengthAdjust x)
           :else x))
   svg))

(defn inject-dimensions [w h hiccup-svg]
  (transform [1] #(assoc % :style {:width w :height h}) hiccup-svg))


(defn view
  "View a ggplot2 command, expressed in the Clojure representation of R code, in Gorilla REPL. Options can be passed
  in a second argument, if wished, as a map. Supported options are :width of plot (in inches!) and :height. If only
  :width is given then a sensible default height will be chosen."
  ([plot-command] (view plot-command {}))
  ([plot-command options]
   (let [width (or (:width options) 6.5)
         height (or (:height options) (/ width 1.618))
         w (* 72 width)
         h (* 72 height)]
     ^:R [:div.rggplot
        ;[:p/html (render plot-command options)]
          (->> (render plot-command options)
               (convert-style-as-strings-to-map)
               (fix-case-tags)
               (inject-dimensions w h))])))



(comment

  ;; https://stackoverflow.com/questions/6535927/how-do-i-prevent-rplots-pdf-from-being-generated  
  ;; pdf (NULL)



  (ts/parse-string (slurp "/tmp/gg4clj2773607344336357351.svg"))

  (transform [1 :a even?]
             inc
             [{:a 1} {:a 2} {:a 3} {:a 4}])


  (inject-dimensions
   [:svg {:viewbox "0 0 288.00 144.00"}
    [:circle]
    [:rect]] 300 200)

  ;comment end
  )