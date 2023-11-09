# righttypes - Because computing mismatches is more useful than asking if (valid? x).

* Not a framework - Just some functions / macros that stand on their own.
* Doesn't try to \"boil the ocean\" or be the One Type Library to Rule Them All.
* Coexists well with and enhances other Clojure \"type\" libraries including Specs and Malli.
* Totally transparent to the rest of your code.
* Built on the idea that any predicate implicitly defines a type--a set of \"things\".
* Java type interop included.
* Integrates well with :pre, :post, and (assert ...).
* Implemented in barely a page of code with 0 dependencies.

Imagine that a type constructor is just a function that returns its input
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

If the value(s) passed to the type constructor conforms to `predicate`, the
type constructor function returns the original value as if it were the identity function.

Otherwise it returns a `TypeCtorError` with detailed information on the failure.
