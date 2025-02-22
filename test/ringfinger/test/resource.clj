(ns ringfinger.test.resource
  (:use (ringfinger auth core resource db fields), ringfinger.db.inmem,
        midje.sweet, ring.mock.request)
  (:import org.apache.commons.codec.binary.Base64))

(make-user inmem :ringfinger_auth {:username "test"} "demo" "salt")

(defresource todos
  {:db inmem
   :pk :body
   :whitelist '(:state)}
  [:body (required) "should be present"])

(defresource hooked
  {:db inmem
   :pk :name
   :whitelist '(:ondata :onpost :onput)
   :hooks {:data    #(assoc % :ondata "yo")
           :create  #(assoc % :onpost "posted")
           :update  #(assoc % :onput  "put")}}
  [:name (required) ""])

(defresource owned
  {:db inmem
   :pk :name
   :owner-field :owner}
  [:name (required) "hey where's the name?"])

(defresource forbidden
  {:db inmem
   :pk :name
   :forbidden-methods [:delete]}
  [:name (required) ""])

(defapp testapp
  {:static-dir "src"
   :fixed-salt "salt"}
  todos, hooked, owned, forbidden)

(defn authd [req]
  (header req "Authorization" (str "Basic " (Base64/encodeBase64String (. "test:demo" getBytes)))))

(facts "about creating"
  (testapp (body (request :post "/todos?_format=json")
                           {:body  "test"
                            :state "false"}))
       => (contains {:status 201
                     :headers (contains {"Location" "/todos/test?_format=json"})})
  (testapp (body (request :post "/todos?_format=json") {:state "false"}))
       => (contains {:status 400
                     :body "{\"body\":[\"should be present\"]}"})
  (:body (testapp (header (request :post "/todos") "Accept" "application/xml")))
       => "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><response><body><error>should be present</error></body></response>"
  (:status (testapp (body (request :post "/hooked?_format=json") {:name "test"}))) => 201
  (:status (testapp (body (authd (request :post "/owned?_format=json")) {:name "sup"}))) => 201)

(facts "about updating"
  (testapp (body (request :put "/todos/test?_format=json")
                 {:body  "test" :state "on"}))
       => (contains {:status 302 :headers (contains {"Location" "/todos/test?_format=json"})})
  (:status (testapp (body (request :put "/hooked/test?_format=json") {:name "test2"}))) => 302
  (:status (testapp (body (request :put "/owned/sup?_format=json") {:name "hacked"})))  => 403
  (:status (testapp (body (authd (request :put "/owned/sup?_format=json")) {:name "wassup"}))) => 302)

(facts "about reading"
  (:body (testapp (request :get "/todos/test?_format=json"))) => "{\"state\":\"on\",\"body\":\"test\"}"
  (:body (testapp (header (request :get "/todos/test") "Accept" "application/xml")))
          => "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><response><state>on</state><body>test</body></response>"
  (:body (testapp (request :get "/hooked/test2?_format=json")))
          => "{\"onpost\":\"posted\",\"ondata\":\"yo\",\"name\":\"test2\",\"onput\":\"put\"}"
  (:body (testapp (request :get "/owned/wassup?_format=json")))
          => "{\"name\":\"wassup\",\"owner\":\"test\"}")

(facts "about index"
  (:body (testapp (request :get "/todos?_format=json"))) => "[{\"state\":\"on\",\"body\":\"test\"}]"
  (:body (testapp (request :get "/todos?_format=json&query_state_ne=on"))) => "[]"
  (:body (testapp (header (request :get "/todos") "Accept" "application/xml")))
       => "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><response><entry><state>on</state><body>test</body></entry></response>")

(facts "about deleting"
  (testapp (request :delete "/todos/test?_format=json"))
       => (contains {:status 302 :headers (contains {"Location" "/todos"})})
  (:status (testapp (body (request :post "/forbidden?_format=json") {:name "test"}))) => 201
  (:status (testapp (request :delete "/forbidden/test?_format=json"))) => 405
  (get-one inmem :todos {:body "test"}) => nil)

(reset-inmem-db)
