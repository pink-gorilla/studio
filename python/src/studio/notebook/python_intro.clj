(ns studio.notebook.python-intro
  (:require
   [libpython-clj2.python :as py :refer [as-python as-jvm ->python ->jvm
                                        py. py.. py.-
                                        get-attr call-attr call-attr-kw
                                        get-item ;att-type-map
                                        ;call 
                                       ;  call-kw
                                         initialize!
                                       ; as-numpy 
                                        ; as-tensor
                                        ; ->numpy
                                        run-simple-string
                                        add-module module-dict ; asm
                                        import-module
                                        python-type]]
   [libpython-clj2.require :refer [require-python]]
   [libpython-clj2.metadata :refer [doc]]))

(run-simple-string "print('hey')")

(def bridged (run-simple-string "print('hey')"))

(def main-globals (-> (add-module "__main__")
                      (module-dict)))

main-globals
(keys main-globals)

(def np (import-module "numpy"))
(def ones-ary (call-attr np "ones" [2 3]))
ones-ary
(type ones-ary)

;;; # Usage
;;; Python objects are essentially two dictionaries, one for 'attributes' and one for 'items'. When you use python and use the '.' operator, you are referencing attributes. If you use the '[]' operator, then you are referencing items. Attributes are built in, item access is optional and happens via the __getitem__ and __setitem__ attributes. This is important to realize in that the code below doesn't look like python because we are referencing the item and attribute systems by name and not via '.' or '[]'.

;Execute Some Python
;*out* and *err* capture python stdout and stderr respectively.
(run-simple-string "print('hey')")



; The results have been 'bridged' into java meaning they are still python objects but there are java wrappers over the top of them. 
; For instance, Object.toString forwards its implementation to the python function __str__.

(def bridged (run-simple-string "print('hey')"))
(instance? java.util.Map (:globals bridged))

(:globals bridged)


; We can get and set global variables here. If we run another string, these are in the environment. 
; The globals map itself is the global dict of the main module:

(def main-globals (-> (add-module "__main__")
                      (module-dict)))
main-globals


(get main-globals "__name__")
(.put main-globals "my_var" 200)
(run-simple-string "print('your variable is:' + str(my_var))")

(def np (import-module "numpy"))
(def ones-ary (call-attr np "ones" [2 3]))


(call-attr ones-ary "__len__")


(vec ones-ary)

(type (first *1))
(get-attr ones-ary "shape")
(vec (get-attr ones-ary "shape"))
;(att-type-map ones-ary)
;att-type-map

; It can be extremely helpful to print out the attribute name->attribute type map:
;(att-type-map ones-ary)



; Errors
; Errors are caught and an exception is thrown.
; The error text is saved verbatim in the exception:
; (run-simple-string "print('syntax errrr")

(py/from-import numpy linspace)
(linspace 2 3 :num 10)

^:R ['user/text (doc linspace)]


; Experimental Sugar
; We are trying to find the best way to handle attributes in order to shorten generic python notebook-type usage. The currently implemented direction is:
; $. - get an attribute. Can pass in symbol, string, or keyword
; $.. - get an attribute. If more args are present, get the attribute on that result.
(py/$. np linspace)

(py/$.. np random shuffle)

; Examples
(require-python '[builtins :as python])
(def xs (python/list))
;xs

(py. xs extend [1 2 3])
(py. xs __len__)
((py.- xs __len__)) ;; attribute syntax to get then call method
(py. xs pop)
(py. xs clear)

; New sugar (fixme)
; libpython-clj offers syntactic forms similar to those offered by Clojure for interacting with Python classes and objects.
; Class/object methods Where in Clojure you would use (. obj method arg1 arg2 ... argN), you can use (py. pyobj method arg1 arg2 ... argN).
; In Python, this is equivalent to pyobj.method(arg1, arg2, ..., argN). Concrete examples are shown below.
; Class/object attributes Where in Clojure you would use (.- obj attr), you can use (py.- pyobj attr).
; In Python, this is equivalent to pyobj.attr. Concrete examples shown below.
; Nested attribute access To achieve a chain of method/attribute access, use the py.. for.
; (Note: requires Python requests module installled)

;; requires Python requests module installed
(require-python 'requests)
(def requests (py/import-module "requests"))
(def h (py.. requests (get "http://www.google.com") -content (decode "latin-1")))
; ^:R ['user/phtml h]

; Numpy
;Speaking of numpy, you can move data between numpy and java easily.
(def tens-data (as-tensor ones-ary))
(println tens-data)


(require '[tech.v2.datatype :as dtype])
(def ignored (dtype/copy! (repeat 6 5) tens-data))
(.put main-globals "ones_ary" ones-ary)


(.put main-globals "ones_ary" ones-ary)

(run-simple-string "print(ones_ary)")



; So heavy data has a zero-copy route. Anything backed by a :native-buffer has a zero copy pathway to and from numpy. 
; For more information on how this happens, please refer to the datatype library documentation.
; Just keep in mind, careless usage of zero copy is going to cause spooky action at a distance.


