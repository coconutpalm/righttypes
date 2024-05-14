(ns righttypes.conversions
  "A 'convert' multimethod that can convert between arbitrary types.  Default implementations
  are supplied for Clojure's built-in types."
  (:require [righttypes.nothing :as nothingness]))


(def Map
  "Alias for clojure.lang.IPersistentMap for use in type conversions"
  clojure.lang.IPersistentMap)

(def Vector
  "Alias for clojure.lang.PersistentVector"
  clojure.lang.PersistentVector)

(def Seq
  "Alias for clojure.lang.ISeq"
  clojure.lang.ISeq)

(def Date
  "Alias for java.util.Date"
  java.util.Date)

(def SqlDate
  "Alias for java.sql.Date"
  java.sql.Date)


(defmulti convert
  "Convert src-instance to dest-class if possible.  Returns nothingness/NO-RESULT-ERROR
  on failure.  For example:

  * (convert Boolean/TYPE \"true\")
  * (convert Map vararg-parameter-kvs)"
  (fn [dest-class src-instance] [dest-class (class src-instance)]))


(defmethod convert [java.io.File String] [_ str]
   (java.io.File. str))


(defmethod convert [Boolean/TYPE String] [_ str]
  (contains? #{"on" "yes" "true"} (.toLowerCase str)))


(defmethod convert [Map Vector] [_ v]
  (assert (even? (count v)) "Vector/Seq must contain an even number of arguments as key-value pairs")
  (apply assoc {} v))

;; What's in a name?
(defmethod convert [clojure.lang.Named Class] [_ c]
  (.getName c))

(defmethod convert [clojure.lang.Named String] [_ x]
  (symbol x))

;; Date handling
(defmethod convert [Long Date] [_ d]
  (.getTime d))
(defmethod convert [Long/TYPE Date] [_ d]
  (.getTime d))

(defmethod convert [Long SqlDate] [_ d]
  (.getTime d))
(defmethod convert [Long/TYPE SqlDate] [_ d]
  (.getTime d))

(defmethod convert [Date Long] [_ l]
  (java.util.Date. l))
(defmethod convert [Date Long/TYPE] [_ l]
  (java.util.Date. l))

(defmethod convert [SqlDate Long] [_ l]
  (java.sql.Date. l))
(defmethod convert [SqlDate Long/TYPE] [_ l]
  (java.sql.Date. l))

(defmethod convert [SqlDate Date] [_ d]
  (java.sql.Date. (.getTime d)))


;; Synonyms...
(defmethod convert [clojure.lang.PersistentArrayMap Vector] [_ v]
  (convert Map v))

(defmethod convert [clojure.lang.PersistentHashMap Vector] [_ v]
  (convert Map v))

(defmethod convert [clojure.lang.PersistentTreeMap Vector] [_ v]
  (convert Map v))

(defmethod convert [Map Seq] [_ s]
  (convert Map (vec s)))

(defmethod convert [Map clojure.lang.ASeq] [_ s]
  (convert Map (vec s)))


(defmethod convert :default [dest-class src-instance]
  (if (.isAssignableFrom dest-class (.getClass src-instance))
    src-instance
    (nothingness/no-result (str "Cannot convert from " (.getClass src-instance) " to " dest-class) src-instance)))
