(ns righttypes.nothing
  "A Nothing type that behaves as an identity value for maps, sequences, and strings under their various
  concatination operations.  (For Category theorists, it's a monoid zero for these types under concatination
  and mapcat.)

  It also provides Nothing constants for cases where Nothing means 'error', 'no result', or simply 'use
  default settings'."
  (:require [righttypes.failure :refer [failure?]]
            [potemkin :refer [def-map-type]]
            [hyperfiddle.rcf :refer [tests]]))

(defprotocol IWhy
  (why [_]))

(def-map-type Nothing [string-value reason]
  IWhy
  (get [_ k default-value]
       default-value)
  (assoc [_ k v] (assoc {} k v))
  (dissoc [_ k] {})
  (keys [_] nil)
  (meta [_] {})
  (with-meta [this mta] this)
  (toString [this] string-value)
  (why [_] reason))

(def nothing
  "Nothing is the value to use when there is nothing to pass or return.  Nothing acts like an
  empty map in a collection context and like an empty string in a string context.

  This has the following implications:

  * You can use it as a result in mapcat when you want nothing appended to the output collection.
  * You can nothing into a value, resulting in a seq containing only the value.
  * You can assoc values into a nothing, resulting in a map.
  * You can conj vector pairs into a nothing, resulting in a map.
  * You can concatinate it with other strings using the str function, adding nothing to the other strings."
  (Nothing. "" {}))

(tests
 (cons 1 nothing)                                       := '(1)
 (assoc nothing :three 3)                               := {:three 3}
 (conj nothing [:two 3])                                := {:two 3}
 (cons 2 nothing)                                       := '(2)
 (str "" nothing)                                       := ""
 (str "foo" nothing)                                    := "foo"
 (mapcat #(if (even? %) [%] nothing) [1 2 3 4])         := '(2 4))

(def NO-RESULT-ERROR
  "An instance of Nothing intended for use as a generic error result.  It is a separate instance
  from 'nothing' because returning 'nothing' might not be an error.  This value is useful for functions
  used within map / mapcat / filter (etc...) chains where it is useful to have an error value that
  behaves like the identity value for a collection.

  However, unlike the nothing value, in a string context, NO-RESULT-ERROR returns an error message.

  NO-RESULT-ERROR is treated as a failure by the failure? multimethod in the errors package."
  (Nothing. "No result error {}" {}))

(defn no-result
  "Create custom Nothingness with a specified (.toString n) and (.why n) value."
  [string-value reason]
  (Nothing. string-value reason))

(def use-defaults
  "A synonmym for nothing for use in parameter lists when passing nothing really means for the
  function to use default values."
  nothing)

(defn Nothing!
  "Return the Nothing type."
  []
  Nothing)

(defn nothing?
  "Returns true if value is an instance of Nothing and false otherwise"
  [value]
  (instance? Nothing value))

(defn something
  "Returns value if value is not nothing ; else returns nil."
  [value]
  (when-not (instance? Nothing value)
    value))

(defmethod failure? [Nothing NO-RESULT-ERROR]
  [_]
  "The 'error' value of the Nothing type is a failure."
  true)

(defn replace-nothing
  "if maybe-value is nil or nothing, return replacement else return value"
  [replacement maybe-value]
  (or (something maybe-value) replacement))

(defn nothing->identity
  "Takes nil or Nothing to the specified identity value for the type and computation in context,
   otherwise returns value.  An identity value can be applied to a value of the given type under the
   operation in context without affecting the result.  For example 0 is the identity value for rational
   numbers under addition.  The empty string is the identity value for strings under concatination.

   Note that Nothing is already an identity value for maps and seqs.  This function is only useful
   for types where the Nothing type is ill-behaved (e.g.: Strings, Numbers, ...) for a given operation.
   
   `identity-value-or-fn` can be a value or a function whose nullary value is the identity value for that function.
   `maybe-value` is replaced with the identity value if it is nil or Nothing.

   Another name for this concept is the monoid zero for the type/operation."
  [identity-value-or-fn maybe-value]
  (let [identity-value (if (fn? identity-value-or-fn) (identity-value-or-fn) identity-value-or-fn)]
    (replace-nothing identity-value maybe-value)))

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
  ([value identity?]
   (when-not (identity? value) value))

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
  (if (something value)
    (f value)
    nothing))
