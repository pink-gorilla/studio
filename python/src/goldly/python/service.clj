(ns goldly.python.service
  (:require
     [taoensso.timbre :refer [info]]
     [goldly.python :refer [py-initialize!]]))

 
(info "initializing python..")
(py-initialize!)

 ; [pinkgorilla.python.plot]) ;; bring to scope 