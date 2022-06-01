(ns studio.python.service
  (:require
     [taoensso.timbre :refer [info]]
     [studio.python :refer [py-initialize!]]))

 
(info "initializing python..")
(py-initialize!)

 ; [pinkgorilla.python.plot]) ;; bring to scope 