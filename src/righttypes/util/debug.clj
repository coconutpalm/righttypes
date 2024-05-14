(ns righttypes.util.debug
  "Some simple utilities for debugging function internals."
  (:require [clojure.pprint :as pp]))


(defn spy
  "pretty-prints `x`, then returns it."
  [x]
  (pp/pprint x)
  x)


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro spy->>
  "Like thread-last, but inserts a (spy) in between each step"
  [& steps]
  (let [new-steps (interpose `(spy) steps)]
    `(->> ~@new-steps)))


(defmacro local-bindings
  "Produces a map of the names of local bindings to their values.
   For now, strip out gensymed locals."
  []
  (let [symbols (remove #(.contains (str %) "_")
                        (map key @clojure.lang.Compiler/LOCAL_ENV))]
    (zipmap (map (fn [sym] `(quote ~sym)) symbols) symbols)))
