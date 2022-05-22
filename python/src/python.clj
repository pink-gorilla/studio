(ns pinkgorilla.python.core
  (:require
   [clojure.datafy :refer [datafy]]
   [libpython-clj.python :as py]
   [pinkgorilla.notebook-app.system]))

(defn py-initialize! []
  (let [config (pinkgorilla.notebook-app.system/get-setting [:python])]
    (println "python config: " config)
    (py/initialize!
     :python-executable (:python-executable config)
     :library-path (:library-path config))))

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