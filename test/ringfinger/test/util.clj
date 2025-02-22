(ns ringfinger.test.util
  (:use ringfinger.util, midje.sweet))

(facts "about call-or-ret"
  (call-or-ret #(str %) 1) => "1"
  (call-or-ret "string" 1) => "string")

(facts "about haz?"
  (haz? [:a :b] :a) => true
  (haz? [:a :b] :c) => false)

(facts "about zeroify"
  (zeroify 9)  => "09"
  (zeroify 10) => "10")

(facts "about nice-count"
  (nice-count 0 "post") => "no posts"
  (nice-count 1 "post") => "one post"
  (nice-count 2 "post") => "two posts"
  (nice-count 99 "problem") => "99 problems")

(let [one 1 two 2]
  (fact (pack-to-map one two) => {:one 1 :two 2}))

(fact (keywordize {"one" 1}) => {:one 1})

(let [mp {:one 1 :two 2}]
  (fact (sorted-zipmap (keys mp) (vals mp)) => mp))

(facts "about sort-maps"
  (let [m [{:a 1 :b 1} {:a 1 :b 2} {:a 2 :b 1} {:a 2 :b 2}]]
    (sort-maps m {:a 1 :b -1}) => '({:a 1 :b 2} {:a 1 :b 1} {:a 2 :b 2} {:a 2 :b 1})
    (sort-maps m {:a -1 :b 1}) => '({:a 2 :b 1} {:a 2 :b 2} {:a 1 :b 1} {:a 1 :b 2})))
