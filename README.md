# righttypes - Because computing mismatches is more useful than asking if (valid? x).

## Why should I care?

Because *knowing* that one's data is clean helps one be confident in one's own code!

*  Not a framework - Just some functions / macros that stand on their own.
*  Doesn't try to \"boil the ocean\" or be the One Type Library to Rule Them All.
*  Coexists with and enhances other Clojure \"type\" libraries including Specs and Malli.
*  Totally transparent to the rest of your code.
*  Implemented in barely a page of code with 0 dependencies.

## Show me some code

Imagine that a type constructor is just a function that returns its input (e.g.: like `identity`) for valid arguments or that either throws `ex-info` with a `TypeCtorError` or simply returns a `TypeCtorError` for invalid arguments.  For example:

```clojure
(def person-categories
  {:peer "Professional peer contact"
   :recruiter "Recruiter"
   :coach "Professional coach"
   :hiring-manager "Hiring Manager"
   :inverview-peer "Potential teammate who interviewed me"})

(def contact-types
  {:in-person "In person"
   :electronic "Phone/fax/email/job website, etc."})

(def Address
  (T {:line1 string?
      (Opt. :line2) string?
      :city string?
      :state string?
      :zip string?}))

(def Person
  (T! {:person-category (set (vals person-categories))
       :contact-type (set (vals contact-types))
       (Opt. :employer-name) string?
       :contact-person string?
       (Opt. :preferred-contact) string?
       :address Address
       (Opt. :phone) string?
       :type-of-business string?
       (Opt. :comment) string?}))

```

In the above code the `T` macro builds a type constructor that always returns a value: either its input (for type matches) or a `TypeCtorError` for mismatches.  The `T!` macro behaves like `identity` for type matches, but throws `ex-info` with a `TypeCtorError` inside `ex-data` for type mismatches.

Careful use of the `T!` macro in code like the above has the effect of checking hard-coded data structures like this at compile time without sacrificing the flexibility that Lispers have come to appreciate.

For example:

```clojure
(def people
  {:brian-caracciolo (Person {:person-category (-> person-categories :recruiter)
                              :contact-type (-> contact-types :electronic)
                              :employer-name "The Cypress Group"
                              :contact-person "Brian Caracciolo"
                              :preferred-contact "https://www.linkedin.com/in/briancaracciolo/"
                              :address (Address {:line1 "1460 Broadway 12th floor"
                                                 :city "New York" :state "NY" :zip "10036"})
                              :type-of-business "Recruiter"})

   :jeremy-streb (Person {:person-category (-> person-categories :recruiter)
                          :contact-type (-> contact-types :electronic)
                          :employer-name "Signify Technology"
                          :contact-person "Jeremy Streb"
                          :preferred-contact "https://www.linkedin.com/in/jeremy-streb/"
                          :address (Address {:line1 "640 N. Sepulveda Blvd #204"
                                             :city "Los Angeles" :state "CA" :zip "90049"})
                          :type-of-business "Recruiter"})})
```

Type constructors of this style integrate seamlessly with ordinary Lisp since their behavior is transparent to downstream operations.  They also encourage rich error checking/reporting and integrate well with other predicate-based "type systems" in Clojure.

## What kinds of things can be predicates?

Constructor arguments are determined to be valid iff `(predicate args)` passes, but with a twist:

*  `predicate` can be a function like in specs.
*  Anything that is valid in function position, like the sets used above, can be a predicate.
*  To validate fixed-length vectors positionally, `predicate` can be a vector of predicates where each predicate validates the value in its corresponding position.
*  To validate a map, `predicate` can be a map in the form `{:key pred?}` or `{(Opt. :key) pred?}` where the `(Opt. :key)` form denotes an optional key.
*  `predicate` can be another type constructor function.
