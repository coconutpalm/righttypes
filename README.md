# RightTypes

In RightTypes, a type constructor is a function that behaves like identity unless it fails.

By expressing types this way, type constructor functions provide machine and human-readable documentation and don't pollute the rest of your code.  

In the exceptional case, a type constructor function returns a value that unambiguously is an error value (containing detailed diagnostics) or throws ex-info with this diagnostic value in ex-data.

Because type constructor functions of this style behave like identity in the normal case, their presence is completely transparent to the rest of your code.

## Guiding principles

*  Not a framework - Just some functions / macros that stand on their own.
*  Doesn't try to \"boil the ocean\" or be the One Type Library to Rule Them All.
*  Coexists with and enhances other Clojure \"type\" libraries including Specs and Malli.
*  Totally transparent to the rest of your code.
*  The core is implemented approximately a page of code.
*  0 dependencies.
*  Thoroughly tested using rich comment form tests that illustrate correct usage.

## Show me some code

```clojure
(def Address
  (T {:line1 string?
      (Opt. :line2) string?
      :city string?
      :state string?
      :zip string?}))
```

`T` is a macro that creates a type constructor function.  Type constructors behave like `identity` when evaluating correctly-formed inputs:

```clojure
user>  (Address {:line1 "1460 Broadway 12th floor"
                 :city "New York" :state "NY" :zip "10036"})
{:line1 "1460 Broadway 12th floor", :city "New York", :state "NY", :zip "10036"}
```

When the input is malformed, it returns detailed error information.

```clojure
user> (Address {:line1 "1460 Broadway 12th floor"
                 :city "New York" :state :NY :zip 42})
{:x {:line1 "1460 Broadway 12th floor", :city "New York", :state :NY, :zip 42},
 :errors [{:pos :state, :msg "(:state string? :NY)"} {:pos :zip, :msg "(:zip string? 42)"}],
 :msg ":state:(:state string? :NY), :zip:(:zip string? 42)",
 :path ()}
```

(The `:path` captures the path to the error if the error is nested somewhere below the top level.)

Let's look at a more detailed example.

```clojure
; First some maps used like enumerations
(def person-categories
  {:peer "Professional peer contact"
   :recruiter "Recruiter"
   :coach "Professional coach"
   :hiring-manager "Hiring Manager"
   :inverview-peer "Potential teammate who interviewed me"})

(def contact-types
  {:in-person "In person"
   :electronic "Phone/fax/email/job website, etc."})

; A "type constructor" that returns diagnostics on failure
(def Address
  (T {:line1 string?
      (Opt. :line2) string?
      :city string?
      :state string?
      :zip string?}))

; A "type constructor" that throws on failure
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

In the above code the `T` macro builds a type constructor function that always returns a value: either its input (for type matches) or a `TypeCtorError` for mismatches.  

In contrast, the `T!` macro behaves like `identity` for type matches, but throws `ex-info` with a `TypeCtorError` inside `ex-data` for type mismatches.

Type mismatch failures bubble up the data structure, so if `Address` fails, `Person` will fail (and `throw`).

Used in data literals, the `T!` macro checks values at compile time without sacrificing the flexibility that Lispers appreciate:

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

In the code above, since the `Person` "type constructor" function is built using the `T!` macro, any invalid `Person` will throw `ex-info` at compile time.

## What kinds of things can be predicates?

Type Constructor arguments are determined to be valid iff `(predicate args)` passes, but with a twist:

*  `predicate` can be another type constructor function.  These are automatically distinguished from ordinary predicates and checked appropriately.  This means that type constructor functions can be nested as with `Address` above.
*  `predicate` can be a function like in specs.
*  `predicate` can be a `java.lang.Class`, which is automatically rewritten as `(fn [x] (instance? TheClass x))`
*  `predicate` can be any value that can also be used as a predicate function, like the sets used in the example above.
*  For validating sequences, `predicate` can be a vector of predicates where each predicate validates the value in the corresponding position.
*  For validating maps, `predicate` can be a map in the form `{:key pred?}` or `{(Opt. :key) pred?}` where the `(Opt. :key)` form denotes an optional key.  (Extra map keys are allowed by default and not checked, but this behavior can be overridden.)

## Is this the only way to build a type constructor function?

No.  There are helper functions for creating other specialized kinds of type constructors.  For example, `seq-of` describes a type constructor function where each element in a `Seqable` collection must satisfy a predicate.

You can write your own functions that buiild type constructors too.  For details, see the source code and tests.

## What do failure messages look like?

The `T` and `T!` macros automatically capture and stringify your source code so it can be used as part of the diagnostics that they generate on failure.

```clojure
user> (def Dice (T #{1 2 3 4 5 6}))
#'user/Dice
user> (-> (Dice 0) :errors)
[{:pos nil, :msg "(#{1 4 6 3 2 5} 0)"}]
```

Within collections, type constructors automatically capture the path to the offending elements as well as their position(s) within the collection.

Let's redefine `Person` above so it returns an error rather than throws an exception, then invoke it from the REPL with some missing keys so we can see the diagnostics:

```clojure
user> (def Person
        (T {:person-category (set (vals person-categories))
            :contact-type (set (vals contact-types))
            (Opt. :employer-name) string?
            :contact-person string?
            (Opt. :preferred-contact) string?
            :address Address
            (Opt. :phone) string?
            :type-of-business string?
            (Opt. :comment) string?}))

user> (Person {:employer-name "The Cypress Group"
               :contact-person "Brian Caracciolo"
               :preferred-contact "https://www.linkedin.com/in/briancaracciolo/"
               :address (Address {:line1 "1460 Broadway 12th floor"
                                  :city "New York" :state "NY" :zip "10036"})
               :type-of-business "Recruiter"})
{:x
 {:employer-name "The Cypress Group",
  :contact-person "Brian Caracciolo",
  :preferred-contact "https://www.linkedin.com/in/briancaracciolo/",
  :address {:line1 "1460 Broadway 12th floor", :city "New York", :state "NY", :zip "10036"},
  :type-of-business "Recruiter"},
 :errors
 [{:pos :contact-type, :msg ":contact-type (set (vals contact-types))"}
  {:pos :person-category, :msg ":person-category (set (vals person-categories))"}],
 :msg "Missing k/v(s): :contact-type (set (vals contact-types)), :person-category (set (vals person-categories))"}
```

## Status of this library

I've now used this library for several small production projects, and it has exceeded my expectations.  Please let me know what works well for you and what can be improved.  Of course, pull requests are welcome!
