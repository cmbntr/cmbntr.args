(defproject cmbntr/cmbntr.args "1.0.2"
  :min-lein-version "2.0.0"

  :description "commandline argument parsing (with commons-cli)"
  :url "https://github.com/cmbntr/cmbntr.args"
  :scm {:url "git@github.com:cmbntr/cmbntr.args.git"}
  :pom-addition [:developers [:developer
                              [:name "Michael Locher"]
                              [:email "cmbntr@gmail.com"]
                              [:timezone "1"]]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [commons-cli "1.2"]]
  :aot [cmbntr.args.main]
  :main cmbntr.args.main)

