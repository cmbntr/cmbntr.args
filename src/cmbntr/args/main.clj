(ns cmbntr.args.main
  (:gen-class)
  (:use [cmbntr.args :only [with-opts-dispatch opt common-opts]]))

(def opts-spec
  (merge common-opts
         {:name     [\n "your name" :args 1]
          :quiet    [\q "be quiet"]}))

(defn -main [& args]
  (with-opts-dispatch opts-spec args
    (if-not (opt :quiet)
      (println (str "Hello, " (opt :name "world"))))))
