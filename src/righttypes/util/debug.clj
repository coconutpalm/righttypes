(ns righttypes.util.debug
  "Some simple utilities for debugging function internals."
  (:require [clojure.pprint :as pp]))

(defmacro local-bindings
  "Produces a map of the names of local bindings to their values.
   For now, strip out gensymed locals."
  []
  (let [symbols (remove #(.contains (str %) "_")
                        (map key @clojure.lang.Compiler/LOCAL_ENV))]
    (zipmap (map (fn [sym] `(quote ~sym)) symbols) symbols)))

(def ^{:doc "The function used to print a value when debugging.
             Defaults to `pprint`.  `tap>` is another useful option."
       :dynamic true} 
  *spy-fn* pp/pprint)

(defn spy
  "emits `x` using `*spy-fn*`, then returns it."
  [x]
  (*spy-fn* x)
  x)

(defmacro spy->>
  "Like thread-last, but inserts a `(spy)` call in between each step"
  [& steps]
  (let [new-steps (interpose `(spy) steps)]
    `(->> ~@new-steps)))

(defmacro spy->
  "Like thread-first, but inserts a `spy` call in between each step"
  [& steps]
  (let [new-steps (interpose 'spy steps)]
    `(-> ~@new-steps)))
