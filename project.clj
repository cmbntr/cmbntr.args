(defproject cmbntr/cmbntr.args "1.0.1"
  :description "commandline argument parsing (with commons-cli)"
  :url "https://github.com/cmbntr/cmbntr.args"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [commons-cli "1.2"]]
  :aot [cmbntr.args.main]
  :main cmbntr.args.main)
