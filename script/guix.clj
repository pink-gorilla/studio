(ns guix
  (:require [babashka.tasks :refer [shell]]))


(defn sudo
  [command & args]
  (println "running sudo " command args)
  (apply shell "sudo" "bb" command args))

(defn extra-path-env [result? var extra]
  (let [current (System/getenv var)
        appended (str current ":" extra)
        opts {:extra-env (assoc {} var appended)}]
    (println "guix/extra path: " appended)
    (if result?
      (merge opts {:out :string
                   :err :inherit})
      opts)))

(defn guile
  [& args]
  (let [opts (extra-path-env false "GUILE_LOAD_PATH" "./modules")
        result (apply shell opts "guile" args)]
     ;(println "guix result: " result)
     ;(println "guix out result: "  (:out result))
    result))

(defn guix
  [command & args]
  (let [opts (extra-path-env false "GUILE_LOAD_PATH" "./script/guile")
        result (apply shell opts "guix" command args)]
     ;(println "guix result: " result)
     ;(println "guix out result: "  (:out result))
    result))

(defn guix-result
  [& args]
  (let [opts (extra-path-env true "GUILE_LOAD_PATH" "./modules")
        _ (println "guix " args)
        result (apply shell opts "guix" args)]
     ;(println "guix result: " result)
    (println "guix out result: "  (:out result))
    result))

(defn package
  [name & args]
  (let [filename (str "./script/" name ".scm")
        manifest (str "--manifest=" filename)
        full-args (concat args [manifest])]
    (println "guix package args: " full-args)
    (apply guix "package" full-args)))
