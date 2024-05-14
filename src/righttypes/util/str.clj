(ns righttypes.util.str
  "Utilities for constructing multiline strings in source code."
  (:require [clojure.string :as str]
            [hyperfiddle.rcf :refer [tests]]))


(defn lines
  "Construct a multiline string.

  Calls `str` on each argument, interposing `\n` in between."
  [& xs]
  (cond
    (nil? xs)    ""
    (seq? xs)    (apply str (interpose "\n" xs))
    :else          (throw (ex-info "Expected nil, a string, or a seq of strings"
                                   {:arg xs
                                    :found (class xs)}))))

(tests
  (lines)           := ""
  (lines "the"
         "quick")   := "the\nquick"
  (lines 1)         := "1"
  (lines 1 [:one])  := "1\n[:one]"
  ,)


(defn strip-margin
  "Like `lines`, but if an argument is already a multiline string, subsequent lines can utilize a
  pipe character ('|') to mark the left margin.  For these arguments, `strip-margin` removes
  spaces and the leading pipe character from those subsequent lines.

  For example:

  (def square-source
    (strip-margin
     \"(defn square [x]
     |  (* x x))
     |
     |(square \" x \")\"))
  "
  [& xs]
  (let [s (apply str (interpose "\n" xs))]
    (str/join "\n"
              (map
               #(str/replace % #" +\|" "")
               (str/split-lines s)))))

(tests
  (strip-margin)          := ""
  (strip-margin "the
                |quick")  := "the\nquick"
  (strip-margin 1 [:one]) := "1\n[:one]"

  ,)
