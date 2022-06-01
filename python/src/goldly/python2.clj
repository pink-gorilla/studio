(ns goldly.python2
  (:require
   [clojure.java.io :refer [as-file #_file]]
   [libpython-clj.require :refer [require-python]]
   [libpython-clj.python :as py :refer [py. #_py.. #_py.-]]
   [pinkgorilla.ui.image :refer [image-view]]
   [pinkgorilla.python.core :refer [py-initialize!]])
  (:import java.awt.image.BufferedImage)  ; bring BufferedImage renderer to scope
  (:import [java.awt Graphics2D #_Image #_Color])
  (:import [java.awt.image BufferedImage #_BufferedImageOp])
  (:import [javax.imageio ImageIO #_IIOImage #_ImageWriter #_ImageWriteParam])
  (:import java.io.File
           java.io.InputStream
           #_java.net.URI
           java.net.URL
           javax.imageio.ImageIO
           java.awt.image.BufferedImage))

(py-initialize!)

; stolen from:
; https://github.com/mikera/imagez/blob/develop/src/main/clojure/mikera/image/core.clj


(defn new-image
  "Creates a new BufferedImage with the specified width and height.
   
   Uses BufferedImage/TYPE_INT_ARGB format by default.
   Option type-or-alpha may be provided, which supports the following values:
    - Boolean: true gives BufferedImage/TYPE_INT_ARGB, false gives BufferedImage/TYPE_INT_RGB.
    - Integer: Specifies the exact image type, e.g. BufferedImage/TYPE_USHORT_GRAY
    - Any other value: Gives the default image type BufferedImage/TYPE_INT_ARGB
   Note that imagez assumes arguments are of BufferedImage/TYPE_INT_ARGB. Operations on other image
   types may not work correctly."
  (^java.awt.image.BufferedImage [width height]
   (new-image width height true))
  (^java.awt.image.BufferedImage [width height type-or-alpha?]
   (cond
     (number? type-or-alpha?) (BufferedImage. (int width) (int height) (int type-or-alpha?))
     (false? type-or-alpha?) (BufferedImage. (int width) (int height) BufferedImage/TYPE_INT_RGB)
     :else (BufferedImage. (int width) (int height) BufferedImage/TYPE_INT_ARGB))))

(defn ensure-default-image-type
  "If the provided image is does not have the default image type
  (BufferedImage/TYPE_INT_ARGB) a copy with that type is returned."
  (^java.awt.image.BufferedImage [^BufferedImage image]
   (if (= BufferedImage/TYPE_INT_ARGB (.getType image))
     image
     (let [copy (new-image (.getWidth image) (.getHeight image))
           ^Graphics2D g (.getGraphics copy)]
       (.drawImage g image nil 0 0)
       copy))))

(defprotocol ColourConversion
  "Coerce different colour representations to an ARGB colour stored in a Long"
  (as-argb [c]))

(defprotocol ImageResource
  "Coerce different image resource representations to BufferedImage."
  (as-image [x] "Coerce argument to an image."))

(extend-protocol ImageResource
  String
  (as-image [s] (ImageIO/read (as-file s)))

  File
  (as-image [f] (ImageIO/read f))

  URL
  (as-image [r] (ImageIO/read r))

  InputStream
  (as-image [r] (ImageIO/read r))

  BufferedImage
  (as-image [b] b))

(defn load-image
  "Loads a BufferedImage from a string, file or a URL representing a resource
  on the classpath.
  Usage:
    (load-image \"/some/path/to/image.png\")
    ;; (require [clojure.java.io :refer [resource]])
    (load-image (resource \"some/path/to/image.png\"))"
  (^java.awt.image.BufferedImage [resource] (ensure-default-image-type (as-image resource))))

#_(defn load-image-resource
    "Loads an image from a named resource on the classpath.
   Equivalent to (load-image (clojure.java.io/resource res-path))"
    (^java.awt.image.BufferedImage [res-path] (load-image (resource res-path))))


;; stolen from:
;; http://gigasquidsoftware.com/

;;; This uses the headless version of matplotlib to generate a graph then copy it to the JVM
;; where we can then print it

;;;; have to set the headless mode before requiring pyplot


(def mplt (py/import-module "matplotlib"))
(py. mplt "use" "Agg")
(require-python 'matplotlib.pyplot)
(require-python 'matplotlib.backends.backend_agg)
(require-python 'numpy)

(defmacro with-show
  "Takes forms with mathplotlib.pyplot to then show locally"
  [& body]
  `(let [tempfile# (File/createTempFile "py_gorilla_plot" ".png")
         path#     (.getAbsolutePath tempfile#)
         _# (matplotlib.pyplot/clf)
         fig# (matplotlib.pyplot/figure)
         agg-canvas# (matplotlib.backends.backend_agg/FigureCanvasAgg fig#)]
     ~(cons 'do body)
     (py. agg-canvas# "draw")
     (matplotlib.pyplot/savefig path#)
     (matplotlib.pyplot/close fig#) ; https://stackoverflow.com/questions/21884271/warning-about-too-many-open-figures
     (image-view (load-image tempfile#))))

(comment

  (macroexpand
   (with-show
     (matplotlib.pyplot/plot [[1 2 3 4 5] [1 2 3 4 10]] :label "linear")))

  ;
  )