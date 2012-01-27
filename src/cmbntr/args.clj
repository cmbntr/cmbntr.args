(ns cmbntr.args
  (:import [org.apache.commons.cli Options Option GnuParser HelpFormatter]))

(defprotocol OptSpec
  (opt-key [o])
  (opt-short-name [o])
  (opt-long-name [o])
  (opt-description [o])
  (opt-required [o])
  (opt-cardinality [o])
  (opt-type [o]))

(defn- shift-and-map [shift args]
  (apply hash-map (->> args seq (drop shift))))

(extend-type clojure.lang.IMapEntry
  OptSpec
  (opt-key [o]
    (-> o .key keyword))
  
  (opt-short-name [o]
    (-> o .val first str))
  
  (opt-long-name [o]
    (-> o .key name))

  (opt-description [o]
    (-> o .val second))
  
  (opt-required [o]
    (-> o opt-cardinality first pos?))
  
  (opt-cardinality [o]
    (if-let [args  (->> o .val (shift-and-map 2) :args)]
      (if (number? args) [0 args] args)
      [0 1]))

  (opt-type [o]
    (-> (->> o .val (shift-and-map 2))
        (get :type nil))))

(defn- opts [o]
  (if (instance? Options o) o 
      (let [[& opt-specs] o
            options (Options.)]
        (doseq [o opt-specs]
          (.addOption options (doto (Option. (opt-short-name o)
                                             (opt-description o))
                                (.setLongOpt (opt-long-name o))
                                (.setArgs (second (opt-cardinality o)))
                                (.setRequired (opt-required o))
                                (.setOptionalArg (not (opt-required o)))
                                (.setType (opt-type o)))))
        options)))

(defn print-usage [spec]
  (let [o   (opts spec)
        pw  (java.io.PrintWriter. *out*)
        fmt (HelpFormatter.)]
    (.printUsage fmt pw 80 "" o)
    (.println pw)
    (.printOptions fmt pw 80 o 2 2)))

(defn print-usage-and-exit [spec]
  (print-usage spec)
  (System/exit 0))

(defmulti  opt-value (fn [t _] t))
(defmethod opt-value String [t s] s)
(defmethod opt-value java.net.URI [t s] (java.net.URI/create s))
(defmethod opt-value java.io.File [t s] (java.io.File. s))
(defmethod opt-value Integer [t s] (Integer/valueOf s))
(defmethod opt-value Long [t s] (Long/valueOf s))
(defmethod opt-value Number [t s] (java.math.BigDecimal. s))
(defmethod opt-value java.util.regex.Pattern [t s] (re-pattern s))
(defmethod opt-value Object [t s]
  (clojure.lang.Reflector/invokeStaticMethod t "valueOf" (into-array String [s])))
(defmethod opt-value :default [t s] (read-string s))

(defn- parse [spec args]
  (let [o (opts spec)]
    (try
      (-> (GnuParser.)
          (.parse o (into-array String args))
          .getOptions seq)
      (catch Exception e (print-usage-and-exit o)))))

(defn parse-args [spec args]
  (into {} (for [opt (parse spec args)]
             (let [k (keyword (.getLongOpt opt))
                   p (partial opt-value  (.getType opt))
                   v (if (> (.getArgs opt) 1)
                       (->> opt .getValuesList (map p) vec)
                       (->> opt .getValue p))]
               [k v]))))


(def ^:dynamic *opt-spec* nil)
(def ^:dynamic *args* nil)
(def ^:dynamic *opts* nil)

(defn opt
  ([k] (k *opts*))
  ([k default] (k *opts* default)))

(defmacro with-opts [spec args & body]
  `(let [s# ~spec
         a# ~args
         o# (parse-args s# a#)]
     (binding [*opt-spec* s#
               *args* a#
               *opts* o#]
       ~@body)))


(comment
  
  (let [spec {:long-name [\l "long name argument" :args [1 4] :type Integer]
              :num [\n "how many" :args [1 1] :type Integer]
              :eck [\e "desc" :args 2 :type java.net.URI]
              :boo [\b "desc boo" :args 4]
              :verbose [\v "a bool" :args 1 :type Boolean]}
        args  ["-b" "v1" "[ 1 2 3]" "-l" "9" "-n" "221" "-e3" "-v" "true"]]
    
    (with-opts spec args
      (println *opts*)
      (println (opt :boo))
      (println (opt :eck))
      (println (map class (vals *opts*)))))
  
  )

