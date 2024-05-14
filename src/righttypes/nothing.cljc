(ns righttypes.nothing
  "A Nothing type that behaves as an identity value for maps, sequences, and strings under their various
  concatination operations.  (For Category theorists, it's a monoid zero for these types under concatination
  and mapcat.)

  It also provides Nothing constants for cases where Nothing means 'error', 'no result', or simply 'use
  default settings'."
  (:require [righttypes.failure :refer [failure?]]))


;; Implementation detail


;; Nope might or might not be an error
(definterface Nope)

(defrecord Nothing []
  Nope
  Object
  (toString [_] ""))

(defrecord NothingnessError []
  Nope
  Object
  (toString [_] "Error: Failed to produce a result." {}))


;; API

(def nothing
  "Nothing is the value to use when there is nothing to pass or return.  Nothing acts like an
  empty map in a collection context and like an empty string in a string context.

  This has the following implications:

  * You can use it as a result in mapcat when you want nothing appended to the output collection.
  * You can cons a value into nothing, resulting in a seq.
  * You can assoc values into a nothing, resulting in a map.
  * You can conj vector pairs into a nothing, resulting in a map.
  * You can concatinate it with other strings using the str function, adding nothing to the other strings."
  (Nothing.))


(def NO-RESULT-ERROR
  "An instance of Nothing intended for use as a generic error result.  It is a separate instance
  from 'nothing' because returning 'nothing' might not be an error.  This value is useful for functions
  used within map / mapcat / filter (etc...) chains where it is useful to have an error value that
  behaves like the identity value for a collection.

  However, unlike the nothing value, in a string context, NO-RESULT-ERROR returns an error message.

  NO-RESULT-ERROR is treated as a failure by the failure? multimethod in the `failure` package."
  (NothingnessError.))

(defmethod failure? [Nothing NO-RESULT-ERROR]
  [_]
  "The 'error' value of the Nothing type is a failure."
  true)


(def no-result
  "When Nothing means that no result was produced but this isn't an error in this case."
  (Nothing.))

(def use-defaults
  "A synonmym for nothing for use in parameter lists when passing nothing really means for the
  function to use default values."
  nothing)


(defn nothing?
  "Returns true if value represents nothing and false otherwise"
  [value]
  (instance? Nope value))

(defn something?
  "Returns value if value is not nothing ; else returns nil."
  [value]
  (when-not (instance? Nope value)
    value))


(defn replace-nothing
  "if maybe-value is nil or nothing, return replacement else return value"
  [replacement maybe-value]
  (if (something? maybe-value)
    maybe-value
    replacement))


(defn nothing->identity
  "Takes nil or Nothing to the specified identity value for the type and computation in context,
  otherwise returns value.  An identity value can be applied to a value of the given type under the
  operation in context without affecting the result.  For example 0 is the identity value for rational
  numbers under addition.  The empty string is the identity value for strings under concatination.

  Note that Nothing is already an identity value for maps and seqs.  This function is only useful
  for types where the Nothing type is ill-behaved (e.g.: Strings, Numbers, ...) for a given operation.

  Another name for this concept is the monoid zero for the type/operation."
  [identity-value maybe-value]
  (replace-nothing identity-value maybe-value))


(defn identity->nil
  "Synopsis:
     (identity->nil [])                                  --> nil
     (identity->nil \"\")                                --> nil
     (identity->nil 1 #{1})                              --> nil   ; e.g.: Under multiplication
     (identity->nil \"none\" #{\"nil\" \"none\" \" \"})  --> nil

  If value is empty (for its type's natural definition of empty), returns nil.  Otherwise returns
  value.

  * The `nothing` value (and its synonyms) are identity values
  * Non-numeric values are empty iff (empty? value).
  * Numbers default to zero as their identity value.
  * The identity predicate may optionally be overridden in the second parameter."
  ([value identity-p #_(=> [Any] Bool)]
   (when-not (identity-p value) value))

  ([value]
   (cond
     (nothing? value) nil
     (number? value)    (identity->nil value zero?)
     :else              (identity->nil value empty?))))


(defn translate-nothingness
  "If value is nil or an instance of Nothing, run f and return its result.  Else, return value."
  [value f]
  (if (or (nil? value)
          (nothing? value))
    (f value)
    value))


(defn translate-something
  "If value is not Nothing return (f value), else return nothing."
  [value f]
  (if (something? value)
    (f value)
    nothing))
