(ns righttypes.util.journal
  "Utilities for defining an activity journal data structure."
  (:import [java.util Date]
           [java.text SimpleDateFormat FieldPosition]))

;; # Date-time and activity journaling

;; ## Formatting

;; Date-time formatting is a pain in Java; let's make some sane defaults.

(let [formatter (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss z")]
  (defn format-time [t]
    (str (.format formatter t (StringBuffer.), (FieldPosition. 0)))))

(let [formatter (SimpleDateFormat. "MM/dd/yyyy hh:mm:ss a z")]
  (defn format-time-am-pm [t]
    (str (.format formatter t (StringBuffer.), (FieldPosition. 0)))))

(let [formatter (SimpleDateFormat. "MM/dd/yyyy")]
  (defn format-time-mdy [t]
    (str (.format formatter t (StringBuffer.), (FieldPosition. 0)))))

;; ## AM/PM

;; It would be nice to be able to just write `am` or `pm`,
;; so let's def those symbols

(def am :am)

(def pm :pm)


;; ## A dynamic scope for specifying a default date

;; Define a thread-local context for making date-time values.
;;
;; By default, "today" is the context for date-time values, but this
;; doesn't preserve referential transparency so use with caution,
;; and don't rely on this; I may change it.

(def ^:dynamic
  this-date
  (doto (Date.)
    (.setHours 0)
    (.setMinutes 0)
    (.setSeconds 0)))

;; Now we need a way to override `this-date` for a given scope.
;;
;; Here's a macro that rebinds `this-date` and executes its body inside the new
;; binding scope.

(defn dateMDY
  "Return a date given a month, day, and a year."
  [m d y]
  (doto (.clone this-date)
    (.setDate d)
    (.setMonth (- m 1))
    (.setYear (- y 1900))))


(defmacro date
  "Bind `this-date` to the specified `m` `d` `y` and execute/return `more`.
  Within the minding, the `tm` function will return times within the specified
  date."
  [m d y & more]
  (let [result (if (= (count more) 1) (first more) (vec more))]
    `(binding [this-date (doto (.clone this-date)
                           (.setDate ~d)
                           (.setMonth (- ~m 1))
                           (.setYear (- ~y 1900)))]
       ~result)))

;; Now we can write a function that returns a Java `DateTime` respecting the
;; current `date` that's in scope.

(defn tm
  ([h m] (doto (.clone this-date)
               (.setHours h)
               (.setMinutes m)))
  ([h m am-pm]
   (cond
     (nil? am-pm)  (doto (.clone this-date)
                     (.setHours h) (.setMinutes m))
     (= :am am-pm) (let [hours (if (= h 12)
                                 0
                                 h)]
                     (doto (.clone this-date)
                       (.setHours hours) (.setMinutes m)))
     (= :pm am-pm) (let [hours (if (< h 12)
                                 (+ h 12)
                                 h)]
                     (doto (.clone this-date)
                       (.setHours hours) (.setMinutes m)))
     :else         (throw (ex-info "Specify :am or :pm" {:found am-pm})))))

;; ## Test creating scoped dates

;; We'll test this by formatting a date/time

(format-time-am-pm
 (date 1 10 1968 (tm 00 23)))

;; `date` just returns whatever it is passed.  All it does is set up
;; a dynamic scope for whatever times are created within its scope.
;;
;; If `date` receives a single argument, it returns that argument.
;; If it receives multiple arguments, it returns them as a vector.
;;
;; These arguments don't have to be time values.  They can be anything!
;;
;; `date` just specifies a default date so that time values can be
;; specified within that date.

(date 1 10 1968
  "Dave's birthday"
  (format-time-am-pm (tm 4 30 am)))

(date 9 12 2023
  (format-time-am-pm (tm 1 00 pm)))

(date 11 1 2023
  (format-time-am-pm (tm 12 23 am)))

(date 11 2 2023
  (format-time-am-pm (tm 12 23 pm)))

;; ## Journaling

;; Sometimes it's nice to return a map keyed by date/time, with values
;; that are some arbitrary value.  Let's define a helper function
;; to make this easy.

(defn journal [& items]
  (letfn [(to-map [d] (->> d (partition 2) (map vec) (into {})))]
    (apply merge (map to-map items))))

;; Here's an example

(def d
 (journal
  (date 1 10 1968
    (tm 3 00 am) {:person 1 :info "R,R"}
    (tm 3 30 am) {:person 2 :info "R,R,+"})
  (date 1 11 1968
    (tm 12 00 am) {:person 1 :info "L,R,+"}
    (tm 12 15 am) {:person 3 :info "R,M"}
    (tm 12 30 am) {:person 2 :info "F,B,Z"})))

;; Graphing libraries like to see tabular data, so here's a function
;; to convert journal data into a table.

(defn journal-table [d]
  (map (fn [[k v]] (merge {:time k} v)) d))

(journal-table d)
