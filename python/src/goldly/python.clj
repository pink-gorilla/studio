(ns goldly.python
  (:require
   [taoensso.timbre :refer [info]]
   [clojure.string :refer [trim]]
   [clojure.java.shell :refer [sh]]
   [clojure.datafy :refer [datafy]]
   [modular.config :refer [get-in-config]]
   [libpython-clj2.python :as py]))
 
 (defn python3-path []
   (-> (sh "which" "python3")
       :out
       trim))

(defn py-initialize! []
  (let [python-config (get-in-config [:python])]
    (info "python config: " python-config)
    (py/initialize!
     :python-executable (python3-path) ;(:python-executable python-config)
     :library-path (:library-path python-config))))

(defn- convert
  "extracts useful information from an item of a python namespace
   this exrtraction is useful, as some python items contain clj
   namespace which cannot be serialized with edn.
   "
  [item]
  (if (map? item)
    (select-keys item [:name :doc :type])
    {}))

(defn pydoc
  "shows he documentation of a python namespace
   Make sure you require python-namespace with :bind-ns
     (require-python '[numpy :as numpy :bind-ns])
     (pydoc numpy)
   "
  [py-namespace]
  ^:R ['viz.python/py-doc
       (map convert
            (vals (datafy py-namespace)))])