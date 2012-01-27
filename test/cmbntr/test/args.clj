(ns cmbntr.test.args
  (:use [cmbntr.args])
  (:use [clojure.test]))

(def spec1
  {:long-name [\l "long name argument" :args [1 4] :type Integer]
   :num [\n "how many" :args [1 1] :type Integer]
   :eck [\e "desc" :args 2 :type java.net.URI]
   :boo [\b "desc boo" :args 4]
   :verbose [\v "a bool" :args 1 :type Boolean]
   :reverse [\r "reverse"]})

(def args1
  ["-b" "v1" "[ 1 2 3]" "-l" "9" "-n" "221" "-e3" "-v" "true" "-r"])

(deftest basic-stuff
  (with-opts spec1 args1
    (is (= '[v1 [1 2 3]] (opt :boo)))
    (is (= [9] (opt :long-name [1,2,3])))
    (is (= 221 (opt :num)))
    (is (instance? java.net.URI (opt :eck)))
    (is (= true (opt :verbose)))
    (is (= true (opt :reverse)))))


