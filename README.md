# righttypes

Precisely computing type mismatches is more useful than asking if (valid? x).

* Not a framework - Just some functions / macros that stand on their own.
* Doesn't try to \"boil the ocean\" or be the One Type Library to Rule Them All.
* Coexists well with and enhances other Clojure \"type\" libraries including Specs and Malli.
* Totally transparent to the rest of your code.
* Built on the idea that any predicate implicitly defines a type--a set of \"things\".
* Java type interop included.
* Integrates well with :pre, :post, and (assert ...).
* Implemented in barely a page of code with 0 dependencies.

