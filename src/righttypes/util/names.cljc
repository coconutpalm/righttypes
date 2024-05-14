(ns righttypes.util.names
  "Translate among various naming and string conventions."
  (:require [clojure.string :as str]))


(defn ->js-string-literal
  "Add quotes and character escape to make `s` into a valid Javascript string literal."
  [s]
  (str \"
       (str/escape s {\newline   "\\n"
                      \return    "\\r"
                      \tab       "\\t"
                      \backspace "\\b"
                      \formfeed  "\\f"
                      \'         "\\'"
                      \"         "\\\""
                      \\         "\\\\"})
       \"))

(defn undasherize
  "Replace all instances of '-' or '_' with replacement"
  [replacement value]
  (str/replace (name value) #"[\-_]" replacement))


(defn ->PascalCase
  "Translate s to PascalCase.  Handles hyphenated-names and underscore_names as well as
  names that are already PascalCase."
  [s]
  (->> (str/split (name s) #"[\_-]")
     (map (fn [part]
             (str (str/upper-case (first part))
                  (apply str (rest part)))))
     (str/join)))


(defn ->camelCase
  "Translate argument to camelCase.  Handles hyphenated-names and underscore_names as well as
  names that are already camelCase."
  [s]
  (let [s' (->PascalCase (name s))]
    (str (str/lower-case (first s'))
         (apply str (rest s')))))


(defn getter
  "Translate property-name to its Java getter syntax.  Handles hyphenated-names and underscore_names as well as
  names that are already camelCase or PascalCase."
  [property-name]
  (->> property-name
     ->PascalCase
     (str "get")))


(defn setter
  "Translate property-name to its Java setter syntax.  Handles hyphenated-names and underscore_names as well as
  names that are already camelCase or PascalCase."
  [property-name]
  (->> property-name
     name
     ->PascalCase
     (str "set")))


(defn ->SNAKE_CASE
  "Convert any Named or String object to SNAKE_CASE.  Does not handle camelCase."
  [value]
  (str/upper-case (undasherize "_" (name value))))


(defn ->uppercase-with-spaces
  "Convert - or _ to ' ' and captialize string."
  [value]
  (str/upper-case (undasherize " " (name value))))


(defn dasherize
  "Replace all instance of match with '-' in value."
  [match value]
  (str/replace (str value) match "-"))


(defn lowercase-initial-chars
  "Convert initial characters of s to lower case"
  ([s] (lowercase-initial-chars "" s))
  ([prefix s]
   #?(:cljs (throw (js/Error. "Not implemented.")))

   (if (empty? s)
     s
     (if (Character/isUpperCase (.charAt s 0))
       (lowercase-initial-chars (str prefix (str/lower-case (str (first s))))
                                (.substring s 1))
       (str prefix s)))))


(defn PascalCase->kebab-case
  [s]
  (-> s
     lowercase-initial-chars
     (str/replace #"([A-Z])"
                  (fn [match]
                    (str "-" (str/lower-case (first match)))))))

                                        ;
(defn ->kebab-case
  "Convert to kebab-case.

  Ex. camelCase          -> :camel-case
      PascalCase         -> :pascal-case
      some_name          -> :some-name
      customer.firstName -> :customer.first-name"
  [name]
  (letfn [(un-camel-case [s]
            )])
  (->> name
     (PascalCase->kebab-case)
     (re-seq #"[ _/]*([a-z1-9$\.]*)")   ; Seq of tokens: Leading delimeter + following chars
     (map first)                        ; Take 1st element of each tuple in match seq
     (map #(str/replace % #"[ _/]" "")) ; Eliminate explicit delimeter characters
     (filter #(not (empty? %)))         ; Some tokens will now be empty; throw them out
     (str/join "-")                     ; Back to a string, joined by "-"
     (str/lower-case)))                  ; ...


(defn keywordize
  "Return dasherized keyword from camelCase underscore_names, namespaced/names, etc.
  See the unit tests for the complete set of supported cases.

  Ex. camelCase          -> :camel-case
      some_name          -> :some-name
      customer.firstName -> :customer.first-name"
  [name]
  (->> name
     (->kebab-case)
     (keyword)))


(defn string->keyword
  "Convert string name to a keyword respecting naming-exceptions.
  naming-exceptions is a predicate (or set or other predicate-like thing).
   Ex. some_name -> :some-name"
  ([name naming-exceptions]
   (if-let [name-exception (naming-exceptions name)]
     name-exception
     (keywordize name)))
  ([name]
   (keywordize name)))
