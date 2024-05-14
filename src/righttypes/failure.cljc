(ns righttypes.failure
  "What is an error?  Is nil an error?  (Not always, but...)  Some multimethods for
  extensably describing what is a failure."
  (:require [righttypes.types])         ; (Needed for :import to work below)

  (:import #?(:clj [java.lang Throwable])
           [righttypes.types TypeCtorError]))


;; Extensible failure objects / test multimethod -------------------------------------

(defmulti failure-class?
  "Indicates if objects of (type val) represent failures purely based on their type.
  This is intended to be extended by clients but not called directly by clients.
  Instead, use (failure? val)"
  (fn [val] [(type val)]))


#?(:clj
   (defmethod failure-class? [Throwable] [_]  true))
#?(:cljs
   (defmethod failure-class? [js/Error] [_]   true))

(defmethod failure-class? [TypeCtorError] [_] true)
(defmethod failure-class? :default [_]       false)


(defmulti failure?
  "A multimethod that determines if a computation has resulted in a failure.
  This allows the definition of what constitutes a failure to be extended
  to new types by the consumer.

  An example of how this can function can be extended to new error types
  exists in this namespace where we extend failure? to include timeout errors."
  (fn [val] [(type val) val]))


(defmethod failure? [nil nil]
  [_]
  "Nil is not a failure."
  false)


(defmethod failure? :default
  [val]
  "Ordinary objects are failures if their Java type is a failure-class?."
  (failure-class? val))
