(ns ringfinger.output
  (:use (clojure.contrib json prxml),
        [clojure.contrib.string :only [substring?]]))

(defprotocol Output
  (render [self status data]))

(defmacro errors-or-data [data]
  `(let [errs# (:errors ~data)] (if errs# errs# (:data ~data))))

(def json (reify Output
  (render [self status data]
          {:status  status
           :headers {"Content-Type" "application/json; charset=utf-8"}
           :body    (json-str (errors-or-data data))})))

; I HATE YOU, XML
(defn- to-xml [data]
  (let [errs (:errors data) dt (:data data)]
    (cond errs      (map (fn [k v] [k (map (fn [t] [:error t]) v)]) (keys errs) (vals errs))
          (map? dt) (map vec dt)
          :else     (map (fn [e] [:entry (map vec e)]) dt))))

(def xml  (reify Output
  (render [self status data]
          {:status  status
           :headers {"Content-Type" "application/xml; charset=utf-8"}
           :body    (str "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                         (with-out-str (prxml [:response (to-xml data)])))})))

(deftype HTMLOutput [view default-data] Output
  (render [self status data]
          {:status  status
           :headers {"Content-Type" "text/html; charset=utf-8"}
           :body    (view (merge default-data data))}))

(defn html-output [view dd] (HTMLOutput. view dd))

(defn getoutput [ctype custom]
  ; FIXME: better solution is possible... or not?!
  (let [outputs (merge {"json" json "xml" xml} custom)] ; html before xml
    (first
      (filter identity
              (map #(if (substring? %1 ctype) %2)
                   (keys outputs)
                   (vals outputs))))))
