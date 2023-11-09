;; Support for making function specs DRYer to write

(ns righttypes.tfn
  "WIP: Support for making function specs DRYer to write."
  (:require
   [righttypes.types :as t :refer [T type-ctor? seq-of valid?]]
   [clojure.walk :refer [prewalk-replace]]))


;; Internal macro for resolving vars inside macros ---------------------------------

(defmacro resolved
  "Returns the resolved var for sym; i.e.: sym -> #'sym"
  [sym]
  (let [v (resolve sym)]
    `~v))


(defn args
  "Return f's argument lists.  Public for testability only."
  [f]
  (-> f meta :arglists))



(defn- symbol->spec [symbols specs]
  (apply assoc {} (interleave symbols specs)))



(defn- arglist-str    [arglist] (pr-str arglist))
(defn- arglist-vector [arglist] (flatten arglist))
(defn- argspec        [arglist parameter-specs] (prewalk-replace (symbol->spec (arglist-vector arglist)
                                                                               parameter-specs)
                                                                 arglist))

(defn- type-str       [arglist parameter-specs return-spec] (str "\n(=> "
                                                                 (argspec arglist parameter-specs)
                                                                 " "
                                                                 return-spec
                                                                 ")"))
(defn- typed-docs     [docstring type-str] (str (if (empty? docstring) "" (str docstring "\n")) type-str))


(defn typed-fn
  "Return a defn statement with :pre and :post conditions using specs to typecheck parameters and return value."
  [f parameter-specs return-spec more]

  (valid? (T (seq-of type-ctor?)) parameter-specs)

  (let [[docstring
         arglist
         body]   (if (string? (first more))
         [(first more) (second more) (rest (rest more))]
         [""           (first more)  (rest more)])

        new-docs (typed-docs docstring (type-str arglist parameter-specs return-spec))]

    `(defn ~f ~new-docs ~arglist
       (valid? (T ~parameter-specs) (flatten ~arglist))
       (valid? (T ~return-spec) (do ~@body)))))


(defn annotate-fn
  "Rename the existing f; make a new f with type checking/docs; delegate to the renamed f."
  [f parameter-specs return-spec]

  (valid? (T (seq-of type-ctor?)) parameter-specs)

  (let [resolved-f (resolve f)
        f-renamed (symbol (str f `-renamed#))
        docstring (-> resolved-f meta :doc)
        arglist (-> resolved-f args first)
        new-docs (typed-docs docstring (type-str arglist parameter-specs return-spec))]

    `(do
       (def ~f-renamed ~f)
       (defn ~f ~new-docs [& args#]
         (valid? (T ~parameter-specs) (flatten args#))
         (valid? (T ~return-spec) (apply ~f-renamed args#))))))


(def function-name (T symbol?))

(defmacro tfn
  "Define a typed function or redefine an existing function to be typed.

  The function's name and type signature are first.  If nothing else follows, the specified
  function must already exist and it will be redefined with the pre/post conditions specified
  by the parameter specs and return spec.  If a function definition (in defn form) follows,
  emits new defn containing pre/post conditions as specified by the parameter specs and return
  spec.

  The metadata map is supplied by this macro and cannot currently be overridden/supplemented.
  Only handles a single argument list.

  In all cases, includes the derived function type in the function's docstring.

  f -
  The function to define.

  parameter-specs -
  A vector of the specs to use to validate each (destructured) argument.  For example, if the
  argument list is [[x y] color] then the parameter spec must have three elements, corresponding
  to x, y, and color.  e.g.: [number? number? keyword?]  The parameter spec is always flattened,
  with the arguments listed in the order in which they appear in the parameter list.

  return-spec -
  A spec to validate the return value.

  more -
  If present, in the form: docstring? [arglist-vector] & statements.  If empty, then annotates
  an existing function f with a type defined by parameter-specs => return-spec."

  [f parameter-specs return-spec & more]

  (letfn [(trace [& xs] (apply str *ns* (seq (vec (meta &form))) ": " (apply pr-str xs)))]
    (try
      (valid? (T symbol?) f)
      (valid? (T (seq-of type-ctor?)) parameter-specs)
      (valid? (T type-ctor?) return-spec)

      (catch AssertionError e
        (throw (AssertionError. (trace (.getMessage e)) e)))))

  (if (empty? more)
    (annotate-fn f parameter-specs return-spec)
    (typed-fn f parameter-specs return-spec more)))


(defmacro fspec
  "Like `tfn` but only annotates existing functions with type information/checking."
  [f parameter-specs return-spec]

  (letfn [(trace [& xs] (apply str *ns* (seq (vec (meta &form))) ": " (apply pr-str xs)))]
    (try
      (valid? (T symbol?) f)
      (valid? (T (seq-of type-ctor?)) parameter-specs)
      (valid? (T type-ctor?) return-spec)

      (catch AssertionError e
        (throw (AssertionError. (trace (.getMessage e)) e)))))

  (annotate-fn f parameter-specs return-spec))
