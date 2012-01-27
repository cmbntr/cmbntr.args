(ns cmbntr.args
  (:import [org.apache.commons.cli Options Option GnuParser HelpFormatter]))

(def ^:dynamic *opt-spec* nil)
(def ^:dynamic *args* nil)
(def ^:dynamic *opts* nil)

(defprotocol OptSpec
  (opt-key [o])
  (opt-short-name [o])
  (opt-long-name [o])
  (opt-description [o])
  (opt-required [o])
  (opt-cardinality [o])
  (opt-type [o]))

(defn shift-and-map [shift args]
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
      [0 0]))

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

(defn print-usage
  ([] (print-usage *opt-spec*))
  ([spec]
     (let [o   (opts spec)
           pw  (java.io.PrintWriter. *out*)
           fmt (HelpFormatter.)]
       (.printUsage fmt pw 80 "" o)
       (.println pw)
       (.printOptions fmt pw 80 o 2 2)
       (.flush pw))))

(defn print-usage-and-exit
  ([] (print-usage-and-exit *opt-spec*))
  ([spec]
     (print-usage spec)
     (System/exit 0)))

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
                   v (condp = (.getArgs opt)
                       0 true
                       1 (->> opt .getValue p)
                       (->> opt .getValuesList (map p) vec))]
               [k v]))))


(defn opt
  ([k] (k *opts*))
  ([k default] (k *opts* default)))

(defn opt? [k]
  (or (opt k) (contains? *opts* k)))

(defmacro with-opts [spec args & body]
  `(let [s# ~spec
         a# ~args
         o# (parse-args s# a#)]
     (binding [*opt-spec* s#
               *args* a#
               *opts* o#]
       ~@body)))

(def common-opts
  {:help [\h "displays this message" :action #(print-usage-and-exit)]})

(defmacro with-opts-dispatch [spec args & body]
  `(let [s# ~spec]
     (with-opts s# ~args
       (doseq [k# (keys *opts*)]
         (if-let [action# (->> (get s# k#) (shift-and-map 2) :action)]
           (action#)))
       ~@body)))
