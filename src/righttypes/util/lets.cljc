(ns righttypes.util.lets
  "`let` variants that return their bindings as a map."
  (:require [hyperfiddle.rcf :refer [tests]]))


(defmacro let-map
  "A version of let that returns its local variables in a map.
  If a result is computed in the body, and that result is another map,
  let-map returns the result of conj-ing the result map into the let
  expression map.  Otherwise it returns a vector containing the let
  expression map followed by the result."
  [var-exprs & body]
  (let [vars (map (fn [[var form]] [(keyword var) var]) (partition 2 var-exprs))
        has-body (not (empty? body))]
    `(let [~@var-exprs
           result# (do ~@body)
           mapvars# (into {} [~@vars])]
       (if ~has-body
         (if (map? result#)
           (conj mapvars# result#)
           [mapvars# result#])
         mapvars#))))

(tests
  "let-map use-cases.  The second one is pretty uncommon."

  (let-map
      [x 1
       y 2
       z (+ x y)])  := {:x 1 :y 2 :z 3}

  (let-map
      [x 1
       y 2
       z (+ x y)]
    (str x y z))    := [{:x 1 :y 2 :z 3} "123"])


(defmacro letfn-map
  "A version of letfn that returns its functions in a map.
  If a result is computed in the body, and that result is another map,
  fn-map returns the result of conj-ing the result map into the function
  map.  Otherwise it returns a vector containing the function map
  followed by the result."
  [fn-exprs & body]
  (let [fn-refs (map (fn [f] [(keyword (first f)) (first f)]) fn-exprs)
        has-body (not (empty? body))]
    `(letfn [~@fn-exprs]
       (let [result# (do ~@body)
             mapfns# (into {} [~@fn-refs])]
         (if ~has-body
           (if (map? result#)
             (conj mapfns# result#)
             [mapfns# result#])
           mapfns#)))))

(tests
  "letfn-map"

  (let [map (letfn-map [(f [x] (* 2 x))])
        g (:f map)]
    (g 16))                       := 32)


(tests "ns loaded"
  (println "\n" *ns* "loaded."))
