(ns helper.str
  (:require [hyperfiddle.rcf :refer [tests]]))


(defn lines
  "Construct a multiline string.

  Calls `str` on each argument, interposing `\n` in between."
  [& strs]
  (cond
    (nil? strs)    ""
    (seq? strs)    (apply str (interpose "\n" strs))
    :else          (throw (ex-info "Expected nil, a string, or a seq of strings"
                                   {:arg strs
                                    :found (class strs)}))))

(tests
  (lines)           := ""
  (lines "the"
         "quick")   := "the\nquick"
  (lines 1)         := "1"
  (lines 1 [:one])  := "1\n[:one]"
  ,)
