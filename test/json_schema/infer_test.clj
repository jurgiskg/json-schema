(ns json-schema.infer-test
  (:require [clojure.test :refer :all]
            [json-schema.core :as v]
            [json-schema.infer :as t])
  (:import clojure.lang.ExceptionInfo))

(deftest infer-map
  
  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type :object
          :additionalProperties false
          :properties {"thing" {:type :integer}}
          :required ["thing"]}
         (t/infer-strict {} {:thing 1} {:title "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type :object
          :additionalProperties false
          :properties {"thing" {:type :object
                                :additionalProperties false
                                :properties {"quantity" {:type :integer}}
                                :required ["quantity"]}}
          :required ["thing"]}
         (t/infer-strict {} {:thing {:quantity 1}} {:title "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type :object
          :additionalProperties false
          :properties {"thing" {:type :object
                                :additionalProperties false
                                :properties {"quantity" {:type :number}}
                                :required ["quantity"]}}
          :required ["thing"]}
         (t/infer-strict {} {:thing {:quantity 1.1}} {:title "ent-1"})))
  
  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type :object
          :additionalProperties false
          :properties {"thing" {:type :object
                                :additionalProperties false
                                :properties {"quantity" {:type :string}}
                                :required ["quantity"]}}
          :required ["thing"]}
         (t/infer-strict {} {:thing {:quantity "11.111,11"}} {:title "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type :object
          :additionalProperties false
          :properties {"things" {:type :array
                                 :items {:type :object
                                         :additionalProperties false
                                         :properties {"quantity" {:type :integer}}
                                         :required ["quantity"]}}}
          :required ["things"]}
         (t/infer-strict {} {:things [{:quantity 1} {:quantity 2}]} {:title "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type :object
          :additionalProperties false
          :properties {"thing" {:type :object
                                :additionalProperties false
                                :properties {"quantities" {:type :array
                                                           :items {:type :number}}}
                                :required ["quantities"]}}
          :required ["thing"]}
         (t/infer-strict {} {:thing {:quantities [1.3 2.2 3.1]}} {:title "ent-1"}))))

(deftest infer-array

  (is (= {:$schema "http://json-schema.org/draft-07/schema#",
          :title "ent-1"
          :type :array
          :items {:type :integer}}
         (t/infer-strict {} [1] {:title "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#",
          :title "ent-1"
          :type :array
          :items {:type :number}}
         (t/infer-strict {} [1.2] {:title "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#",
          :title "ent-1"
          :type :array
          :items {:type :string}}
         (t/infer-strict {} ["hej"] {:title "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#",	  
          :title "ent-1",
          :type :array,
          :items
          {:type :object,
           :additionalProperties false,
           :properties {"quantity" {:type :integer}},
           :required ["quantity"]}}
         (t/infer-strict {} [{:quantity 1}] {:title "ent-1"}))))

(deftest infer-strict->json
  (is (= "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"title\":\"ent-1\",\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"thing\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"quantities\":{\"type\":\"array\",\"items\":{\"type\":\"number\"}}},\"required\":[\"quantities\"]}},\"required\":[\"thing\"]}"
         (t/infer->json {} {:thing {:quantities [1.3 2.2 3.1]}} {:title "ent-1"}))))

(deftest infer-and-validate
  (let [data {:things [{:quantity 1} {:quantity 2}]
              :yearly {2020 1
                       2021 2
                       2022 2.5
                       2023 3
                       2024 (/ 1 3)}}
        schema (t/infer-strict {} data {:title "ent-1"})]
    (is (= data (v/validate schema data)))))

(deftest infer-non-strict
  (let [data {:things [{:quantity 1} {:quantity 2}]
              :yearly {2020 1
                       2021 2
                       2022 2.5
                       2023 3
                       2024 (/ 1 3)}
              :meta {:this "is"
                     :optional? "yesyes"}}
        schema (t/infer-strict {:optional #{:meta}}
                               data
                               {:title "ent-1"})]
    (is (= data (v/validate schema data)))
    (is (= (dissoc data :meta) (v/validate schema (dissoc data :meta))))
    (is (thrown? ExceptionInfo data (v/validate schema (assoc data :meta2 {:this "is" :not "allowed"}))))))
