(ns righttypes.types
  "A very Lispy way to imagine types.

  * Built on the idea that any predicate implicitly defines a type--a set of \"things\".
  * Java type interop included.
  * Not a framework - Just some functions / macros that stand on their own.
  * Doesn't try to \"boil the ocean\" or be the One Type Library to Rule Them All.
  * Coexists well with and enhances other Clojure \"type\" libraries, particularly Specs.
  * Totally transparent to the rest of your code.
  * Integrates well with :pre, :post, and (assert ...).
  * Implemented in barely a page of code with 0 dependencies."
  (:require [clojure.set :as set]
            [clojure.pprint :refer [pprint]]
            [hyperfiddle.rcf :refer [tests]])
  (:gen-class))


;; Add the ::T metadata to thing to mark it as a type constructor
(defn with-ctor-meta
  "Type constructor functions must define `:cljfoundation.types/T` metadata to a truthy value.
  This function returns its input value, with `{::T true}` added to its metadata."
  [thing]
  (with-meta thing {::T true}))

(defn type-ctor?
  "If the specified function contains the ::T metadata, returns true, else returns false."
  [f] (true? (::T (meta f))))

#_{:clj-kondo/ignore [:unused-value]}
(tests
  "type-ctor? identifies type constructor functions via ::cljfouncation.types/T metadata"
  (let [Any (with-ctor-meta identity)
        Any? (fn [_] true)]

    (type-ctor? Any)  := true
    (type-ctor? Any?) := false))


;; Error types ========================================================================

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(definterface ICtorError
  (prependPath [pos type-str])
  (errorPositions))


;;FieldErr {(Opt. :pos) int? :msg string?}
(defrecord FieldErr [pos msg]
  Object
  (toString [_]
    (if pos
      (str pos ":" msg)
      msg)))


;; x - The object with error(s)
;; errors - For a collection: A vector of `TypeCtorError` for each error'd element
;; msg - A human-readable message identifying the expected form and the error appropriate to this level in the collection
;; path - nil if not a collection, else the path to the error within the collection
(defrecord TypeCtorError [x errors msg path]
  :load-ns true

  ICtorError
  (prependPath [^TypeCtorError e pos type-str]
    (TypeCtorError.
     (:x e)
     (:errors e)
     (:msg e)
     (conj (seq (:path e)) (str pos ": " type-str))))

  (errorPositions [this]
    (letfn [(pos [e] (if (:pos e) (:pos e) 0))]
      (vec (map (fn [e] (cond
                         (instance? TypeCtorError e) {(-> e :path first pos) (.errorPositions e)}
                         (map? e)                    (pos e)
                         :else                       (str (type e))))
                (:errors this)))))

  Object
  (toString [this]
    (if (first (:path this))
      (str "{ "
           (apply str "path://" (conj (vec (interpose "/" (:path this))) "/"))
           " [" (:msg this) "]"
           " }")
      (str (:msg this)))))


(defn error-positions
  "Given a `TypeCtorError`, returns the set of top-level positions where `TypeCtorError`s were detected."
  [^TypeCtorError e]
  (set (map :pos (:errors e))))


(defn T->pred
  "Accepts either a predicate or a type-ctor function and returns a traditional predicate.  This helper
  intends to make interop with other typey libraries easier."
  [predicate-or-type-ctor]
  (fn [x]
    (let [result (predicate-or-type-ctor x)]
      (if (instance? TypeCtorError result)
        false
        result))))

(defn ctor-failure?
  "Returns true iff (instance? TypeCtorError result)"
  [result]
  (instance? TypeCtorError result))

;; Test one thing =====================================================================

(defn ^:private maybe-type-error
  "Runs (type-test x) where `type-test` is:

  * A java.lang.Class.  The test is rewritten as a predicate (instance? type-test x).
  * A \"predicate\" indicating if `x` satisfies `type`.
  * A type constructor function

  Returns a vector of 0 or 1 element depending on if an error was detected or not.

  Truthy values except for (instance? TypeCtorError value) satisfy (type-test x) and do
  not return an error value.  (They return an empty vector.)

  Falsy values for (type-ctor? type-test) satisfy (type-test x) and do not return an
  error value.  (They return an empty vector.)  Otherwise type-test is considered
  to be a regular predicate and falsy values return a FieldErr value.

  If type-test returns a TypeCtorError and is positional (maybe-pos is not nil), the
  result is a copy of the TypeCtorError with the positional information prepended
  to its path.

  If type-test returns a TypeCtorError and no positional information is supplied,
  the result is the TypeCtorError."
  [type-test type-str x & [maybe-pos]]
  (let [satisfies? (if (instance? java.lang.Class type-test)
                     #(instance? type-test %)
                     type-test)
        result (satisfies? x)]
    (if (type-ctor? satisfies?)
      (if (instance? TypeCtorError result)
        [(if maybe-pos
           (-> result (.prependPath maybe-pos type-str))
           result)]
        [])
      (if result
        []
        [(let [msg (str "(" type-str " " (pr-str x) ")")]
           (FieldErr. maybe-pos msg))]))))

#_{:clj-kondo/ignore [:unused-value]}
(tests

  (let [Any       (with-ctor-meta identity)
        Any?      (fn [_] true)
        Nothing   (with-ctor-meta (fn [x] (TypeCtorError. x [] "msg" [])))
        Nothing1? (fn [_] nil)
        Nothing2? (fn [_] false)
        Date?     java.util.Date
        d         (java.util.Date.)]

    "Type constructors must explicitly return an error to indicate failure"
    (maybe-type-error Any "Any" 1)                          := []
    (maybe-type-error Any "Any" true)                       := []
    (maybe-type-error Any "Any" false)                      := []
    (maybe-type-error Any "Any" nil)                        := []
    (map class (maybe-type-error Nothing "Nothing" 1))      := [TypeCtorError]
    (map class (maybe-type-error Nothing "Nothing" nil))    := [TypeCtorError]

    "If a predicate returns any falsey value, this indicates a failure"
    (maybe-type-error Any? "Any?" false)                    := []
    (map class (maybe-type-error Nothing1? "Nothing1?" 1))  := [FieldErr]
    (map class (maybe-type-error Nothing2? "Nothing2?" 1))  := [FieldErr]

    "java.lang.Class values are rewritten as a predicate: (instance? type-test x)"
    (maybe-type-error Date? "Date?" d)                      := []
    (map class (maybe-type-error Date? "Date?" "d"))        := [FieldErr]

    ,))


;; Map type constructors ==============================================================


;; A marker for map keys that are optional
(defrecord Opt [key] :load-ns true)

(def
  ^{:dynamic true
    :doc "The default predicate function to use if a map key isn't defined.

This value must be in the form {:pred pred-fn :type-str \"pred-fn-string-representation\"}

Its default value is:

{:pred (fn [_] true) :type-str \"(fn [_] true)\"}

The effect is to not check extra map keys/slots but simply pass them along.  A side-effect of
this default is that optional map keys with typos won't be automatically detected."}
  *undefined-map-key-predicate*
  {:pred (fn [_] true) :type-str "(fn [_] true)"})

(def ^{:doc "A convenience value one can bind to *undefined-map-key-predicate* to disallow unexpected map keys."}
  disallow-unexpected-map-keys {:pred (fn [_] false) :type-str "unexpected-map-key"})


(defmacro disallow-unexpected-map-keys-in
  "A convenience macro to bind `*undefined-map-key-predicate*` to `disallow-unexpected-map-keys`
  in a scope."
  [& scope]
  `(binding [*undefined-map-key-predicate* disallow-unexpected-map-keys]
     ~@scope))


(def ^{:doc "Ensure `m` satisfies k/v predicates in `kv-types` map.  Returns `m` or a TypeCtorError."}
  map-err-T
  (with-ctor-meta
    (fn [kv-types default-predicate]
      (let [required-keys (->> kv-types
                             (seq)
                             (map first)
                             (filter #(not (instance? Opt %)))
                             (set))
            predicates    (into {}
                                (map (fn [[k v]]
                                       (if (instance? Opt k)
                                         [(:key k) v]
                                         [k v]))
                                     kv-types))]

        (with-ctor-meta
          (fn [m]
            (let [missing-keys  (set/difference required-keys (set (keys m)))]
              (if-not (empty? missing-keys)
                (let [errors (->> missing-keys
                                (map (fn [k]
                                       (let [type-str (:type-str (get predicates k default-predicate))]
                                         (FieldErr. k type-str))))
                                (vec))
                      msg (->> missing-keys
                             (map #(get predicates %))
                             (map :type-str)
                             (interpose ", ")
                             (apply str "Missing k/v(s): "))]
                  (TypeCtorError. m errors msg '()))

                (let [errors (vec (mapcat (fn [[k v]]
                                            (let [{:keys [pred type-str]} (get predicates k default-predicate)]
                                              (maybe-type-error pred type-str v k)))
                                          m))]
                  (if (empty? errors)
                    m
                    (TypeCtorError. m
                                    errors
                                    (->> errors
                                       (interpose ", ")
                                       (apply str))
                                    '())))))))))))

#_{:clj-kondo/ignore [:unused-value]}
(tests
  "= map-err-T ="
  (def testee (map-err-T {:one         {:pred string?   :type-str ":one string?"}
                          :two         {:pred int?      :type-str ":two int?"}
                          :three       {:pred rational? :type-str ":three rational?"}
                          (Opt. :four) {:pred string?   :type-str "(Opt. :four) string?"}}
                         *undefined-map-key-predicate*))

  "Happy paths"
  (testee {:one "1" :two 2 :three 5/3})            := {:one "1" :two 2 :three 5/3}
  (testee {:one "1" :two 2 :three 5/3 :four "4"})  := {:one "1" :two 2 :three 5/3 :four "4"}

  "Missing keys"
  (error-positions (testee {:one "1" :two 2}))     := #{:three}
  (error-positions (testee {:two 2}))              := #{:one :three}

  "Misspelled optional key won't be detected"
  (testee {:one "1" :two 2 :three 5/3 :fuur "4"})  := {:one "1" :two 2 :three 5/3 :fuur "4"}

  "Type mismatches"
  (error-positions (testee {:one "1" :two 2 :three 5/3 :four 4}))  := #{:four}
  (error-positions (testee {:one 1/3 :two 2 :three 5/3 :four 4}))  := #{:one :four}
  ,)


;; Sequential type constructors =======================================================

(def ^::T positional-errs
  "Ensure xs satisfies positional predicates in `types'.  Returns xs or a TypeCtorError."
  (with-ctor-meta
    (fn
      [types types-strs xs]
      (letfn [(pairs [as bs] (partition 3 (interleave as bs (range))))] ; (range) adds position-index to (a,b,position-index)

        (if (not= (count types) (count xs))
          (let [msg (str "(count types): " (count types) " (count xs): " (count xs) " types: " types-strs)]
            (TypeCtorError. xs [(FieldErr. nil msg)] msg '()))

          (let [preds  (pairs types types-strs)
                checks (pairs preds xs)
                errors (mapcat (fn [ [[p? pstr] x position] ]
                                 (maybe-type-error p? pstr x position))
                               checks)]
            (if (empty? errors)
              xs
              (TypeCtorError. xs
                              (vec errors)
                              (->> (vec errors)
                                 (interpose ", ")
                                 (apply str))
                              '()))))))))


#_{:clj-kondo/ignore [:unused-value]}
(tests
  "Argument count mismatch"
  (class (positional-errs [number? string?] ["number" "string?"] [5])) := TypeCtorError

  "Happy paths - Behaves as the identity function"
  (positional-errs [] [] [])                                        := []
  (positional-errs [number?] ["number?"] [5])                       := [5]
  (positional-errs [integer? string? keyword? string?]
                   ["integer?" "string?" "keyword?" "string?"]
                   [2006 "Go" :cubs "go!"])                         := [2006 "Go" :cubs "go!"]

  "Positional failure--returns TypeCtorError capturing (0-based) failure position"
  (error-positions (positional-errs [string?] ["string?"] [5]))     := #{0}
  (error-positions (positional-errs [number? ratio? string?]
                                    ["number?" "ratio?" "string?"]
                                    ["5" 3 "Go!"]))                 := #{0 1}
  ,)


(defn ^::T seq-of'
  [p? p?-str]
  (with-ctor-meta
    (fn [xs]
      (let [indexed-xs (partition 2 (interleave xs (range)))
            errors     (mapcat (fn [[x pos]]
                                 (maybe-type-error p? p?-str x pos))
                               indexed-xs)]
        (if (empty? errors)
          xs
          (TypeCtorError. xs
                          (vec errors)
                          (->> (vec errors)
                             (interpose ", ")
                             (apply str))
                          '()))))))

(defmacro seq-of
  "A macro to define a type constructor for a seq where each element must satisfy
  the predicate `p?`"
  [p?]
  (let [p?-str (pr-str p?)]
    `(seq-of' ~p? ~p?-str)))

#_{:clj-kondo/ignore [:unused-value]}
(tests
 "= seq-of ="
 (def N (seq-of number?))

 (N [5/3 3/5 1])                              := [5/3 3/5 1]
 (error-positions (N ["one" 2 "three" 4.0]))  := #{0 2}
 ,)

;; Predicate type constructors ========================================================

(def x-or-err ^::T
  (with-ctor-meta
    (fn [tp type-str x]
      (let [error-result (first (maybe-type-error tp type-str x))]
        (cond
          (nil? error-result)  x
          (map? error-result)  (TypeCtorError. x [error-result] error-result '())
          :else                error-result)))))


;; The `T` macro ======================================================================

(defmacro T
  "Because computing failures is more useful than asking if a value is `specs/valid?`.

  Here we reimagine a type constructor as a function that returns its input
  for valid arguments or that returns a TypeCtorError for invalid arguments.

  Type constructors of this style can integrate seamlessly with ordinary Lisp
  since their behavior is transparent to downstream operations.  They also encourage
  rich error checking/reporting and integrate well with other predicate-based
  \"type systems\" in Clojure.

  Constructor arguments are determined to be valid iff `(predicate args)` is true,
  but with a twist:

  `predicate` can be a function like in specs.

  To validate fixed-length vectors positionally, `predicate` can be a vector of functions
  where each function is a predicate that validates the value in its corresponding
  position.

  To validate a map, `predicate` can be a map in the form {:key pred?} or
  {(Opt. :key) pred?} where the (Opt. :key) form denotes an optional key.
  In both cases values are checked by the `pred?` function.

  And `predicate` can be another type constructor function.

  This macro returns a type constructor function as defined above.

  If the value(s) passed to the type constructor conforms to `predicate`, the
  type constructor function returns the original value as if it were the identity function.

  Otherwise it returns a `TypeCtorError` with information on the failure.

  The result can be checked by asserting that the output of the type constructor
  is the same as its input.

  TypeCtorError is also defined as a `failure?` in the errors namespace."
  [type]
  (let [line-col (vec (meta &form))
        trace    (fn [& xs] (apply str *ns* (seq line-col) ": " (apply pr-str xs)))
        pretty   (fn [x] (if (instance? clojure.lang.Named x) (name x) (pr-str x)))]
    (cond
      (map? type)     (let [kv-types (->> type
                                        (map (fn [[k v]]
                                               [k {:pred v
                                                   :type-str (str (pr-str k) " " (pretty v))}]))
                                        (into {}))]
                        `(map-err-T ~kv-types *undefined-map-key-predicate*))

      (vector? type)  (let [types-strs (vec (map pretty type))]
                        `(with-ctor-meta (partial positional-errs ~type ~types-strs)))

      ;; Named function
      (symbol? type)  (let [type-str (name type)]
                        `(with-ctor-meta (partial x-or-err ~type ~type-str)))

      ;; Anonymous functions
      (list? type)    (let [type-str (pr-str type)]
                        `(with-ctor-meta (partial x-or-err ~type ~type-str)))
      (fn* type)      (let [type-str (pr-str type)]
                        `(with-ctor-meta (partial x-or-err ~type ~type-str)))

      :else           (throw (ex-info (trace "Unrecognized type constructor \"predicate\"") {:type type})))))


(defn assert-satisfies
  "Asserts that `type-ctor` appled to `x` yields a valid value.  Returns `x` if successful else throws AssertionError."
  [type-ctor x]
  (let [maybe-error (type-ctor x)]
    (if (= x maybe-error)
      x
      (throw (AssertionError. (.toString maybe-error))))))


#_{:clj-kondo/ignore [:unused-value :unused-binding]}
(tests
 "Positional T"
 (def FirstMiddleLastName (T [string? string? (fn [s] (string? s))])) ; Ensure lambda error message code path works

 (FirstMiddleLastName ["Charles" "M." "Brown"])              := ["Charles" "M." "Brown"]
 (error-positions
  (FirstMiddleLastName [:Charles "M." 'Brown]))              := #{0 2})

(tests
  "Associative T (maps)"
  (def Person (T {:first string?
                  (Opt. :middle) string?
                  :last string?}))

  (Person {:first "Charles" :last "Brown"})             := {:first "Charles" :last "Brown"}
  (Person {:first "Charles" :middle "M" :last "Brown"}) := {:first "Charles" :middle "M" :last "Brown"}

  (map error-positions
       [(Person {:first 'Charles :last "Brown"})
        (Person {:first 'Charles :middle :m :last "Brown"})]) := [#{:first} #{:first :middle}]

  "By default, misspelled optional keys aren't detected.  Here's how to change that default behavior:"
  (disallow-unexpected-map-keys-in
    (def Person (T {:first string?
                    (Opt. :middle) string?
                    :last string?})))

  (error-positions
   (Person {:first "Charles" :middel "M" :last "Brown"})))  := #{:middel}

(tests
 "Sets can be predicates too!"
 (def OneTwoThree (T #{:one :two :three}))

 (OneTwoThree :one) := :one
 (OneTwoThree :two) := :two
 (OneTwoThree :three) := :three
 (-> (OneTwoThree :four) class) := TypeCtorError)

(tests
 "FUNCTIONS"
 "Test fixture..."
 (defn assert-illinois-drinking-ages [drinking-age]
   (assert-satisfies drinking-age 30)
   (assert-satisfies drinking-age 21)
   (assert (instance? TypeCtorError (drinking-age 20)))
   (assert (instance? TypeCtorError (drinking-age 10))))

 "Named predicate"
 (defn drinking-age-illinois? [age] (and (number? age) (>= age 21)))
 (def DrinkingAgeIllinois (T drinking-age-illinois?))
 (assert-illinois-drinking-ages DrinkingAgeIllinois)

 "Anonymous (fn [x])"
 (def DrinkingAgeIllinois (T (fn [age] (and (number? age) (>= age 21)))))
 (assert-illinois-drinking-ages DrinkingAgeIllinois)

 "Anonymous #(f %)"
 (def DrinkingAgeIllinois (T #(>= % 21)))
 (assert-illinois-drinking-ages DrinkingAgeIllinois))

#_{:clj-kondo/ignore [:unused-value :unused-binding]}
(tests
  "NESTING"

  "- in vectors"
  (def FirstMiddleLastName
                           (T [string? string? string?]))
  (def Address2Lines (T [FirstMiddleLastName string?]))

  (Address2Lines [["First" "M." "Last"] "Line2"])    := [["First" "M." "Last"] "Line2"]
  (-> (Address2Lines [["First" :M "Last"] "Line2"])
     :errors first :errors first :msg)               := "(string? :M)"

  "- in maps"
  (def Sub (T {:two/one string? :two/two number?}))
  (def Nested (T {:one number? :two Sub}))

  (error-positions (Sub {:two/one 1 :two/two 2}))    := #{:two/one}

  (Nested {:one 1 :two {:two/one "one" :two/two 2}}) := {:one 1 :two {:two/one "one" :two/two 2}}
  (Nested {:one 1 :two {:two/one 1 :two/two 2}})
  (-> (Nested {:one 1 :two {:two/one 1 :two/two 2}}) class) := TypeCtorError)


(defmacro T!
  "Like `T` but returns a type constructor that immediately throws ex-info
  if its input value fails to match the specified type.  If the type constructor
  throws, the `ex-data` is the `TypeCtorError` returned by the type constructor.

  Experimental API.  I'm not sure that type constructors should ever throw.
  Maybe this should be an type constructor invocation wrapper instead?"
  [type]
  `(let [ctor# (T ~type)]
     (with-ctor-meta
       (fn [x#]
         (let [result# (ctor# x#)]
           (if (ctor-failure? result#)
             (throw (ex-info "Type construction failure" {:failure result#}))
             result#))))))

#_{:clj-kondo/ignore [:unused-value]}
(tests
  (def Integer! (T! integer?))

  (Integer! 123)         := 123
  (Integer! 4/3)         :throws clojure.lang.ExceptionInfo
  (-> *1 ex-data :failure class) := TypeCtorError

  (def Named! (T! {:first-name string? :last-name string?}))

  (Named! {:first-name "Dave" :last-name "Orme"})  := {:first-name "Dave" :last-name "Orme"}
  (Named! {:first-name "Dave"})                    :throws clojure.lang.ExceptionInfo

  ,)


;; Support {:index-value {:key :index-value}} maps ====================================

(defn- index-by
  "Returns a transducer suitable for transforming a seq of maps into a map of {:key {:id :key}} using `into`.

  (into {} (index-by :key) [{:key :id, ...} ...]"
  [field]
  (map (fn [x] [(get x field) x])))


(defn indexed
  "A type constructor to build a map of maps, where a field in each value-map is also the `index-key`.
  e.g.:

  {:index-value {:key :index-value}}

  EXPERIMENTAL: TODO: Validate result and return TypeCtorError as needed"
  [map-ctor index-key]
  (with-ctor-meta
    (fn [& maps]
      (let [result (cond
                     (nil? maps) {}
                     :else (into {} (index-by index-key) (map map-ctor maps)))]
        result))))


#_{:clj-kondo/ignore [:unused-value :unused-binding]}
(tests
  "Happy path"
  (def Person (T {:key keyword? :first-name string? :last-name string?}))
  (def PersonDB (indexed Person :key))

  (let [testee (PersonDB {:key :franken :first-name "Franken" :last-name "Stein"}
                         {:key :charlie :first-name "Charlie" :last-name "Brown"})]
    (-> testee :franken :last-name) := "Stein")

  "Sadness (TODO: Because for now we're happy all the time)")

(println)
