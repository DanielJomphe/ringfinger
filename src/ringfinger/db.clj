(ns ringfinger.db
  "Document-oriented database abstraction used by ringfinger.resource."
  (:use ringfinger.util,
        [clojure.string :only [split]]))

(defprotocol Database
  (create      [self coll data])
  (create-many [self coll data])
  (get-many    [self coll options])
  (get-one     [self coll options])
  (update      [self coll entry data])
  (delete      [self coll entry]))

; For databases w/o built-in filters, eg. inmem
; TODO: implement more of
; http://www.mongodb.org/display/DOCS/Advanced+Queries
(defn- make-query [kk vv]
  (apply andf (map (fn [k v]
    (condp = k
      :$not    (not (make-query kk v))
      :$lt     (< kk v)
      :$lte    (<= kk v)
      :$gt     (> kk v)
      :$gte    (>= kk v)
      :$exists (if (false? v) (nil? kk) (not (nil? kk)))
      :$ne     (not (= kk v))
               (= kk v))) (keys vv) (vals vv))))

(defn make-filter [query]
  (fn [entry]
    (apply andf (map (fn [k v]
      (if (map? v) (make-query (get entry k) v)
                   (= (get entry k) v))) (keys query) (vals query)))))

(defmacro qs [a] `(keyword (str "$" ~a)))

(defn- param-to-dboptions
  ([q] {(keyword (first q)) (apply param-to-dboptions (rest q))})
  ([k v] {(keyword k) v})
  ([k kk v] {(keyword k) {(qs kk) v}})
  ([k kk kkk v] {(keyword k) {(qs kk) {(qs kkk) v}}})
  ([k kk kkk kkkk v] {(keyword k) {(qs kk) {(qs kkk) {(qs kkkk) v}}}}))
; yeah, that's a mess, but a really fast mess!

(defn params-to-dboptions
;  "Turns ring query-params into db options
;  eg. {'query_field_ne' 3, 'sort_field' -1}
;  becomes {:query {:field {:$ne 3}}, :sort {:field -1}}"
  ([qp] (if (empty? qp) nil (apply merge (map params-to-dboptions (keys qp) (vals qp)))))
  ([q v] (if (substring? "_" q) (param-to-dboptions (flatten (list (split q #"_") (typeify v)))) nil)))
