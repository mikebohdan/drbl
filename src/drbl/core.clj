(ns drbl.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json])
  (:use [slingshot.slingshot :only [try+]]))

(def auth-token "")

(defn get-params
  [page]
  {:query-params {"page" page
                  "per_page" 1000
                  "access_token" auth-token}
   :throw-exceptions false})

(defn get-data
  [url params]
  (let [{body :body
         status :status} (http/get url params)]
    (println (clojure.string/join " " [status url]))
    (case status
      200 body
      429 (do (Thread/sleep 1000)
              (recur url params))
      (throw (Exception. (str "unexpected error " url))))))

(defn query-api
  ([path page body]
   (let [url (str "https://api.dribbble.com/v1" path)
         params (get-params page)
         raw-part (get-data url params)
         part (json/read-str raw-part)]
     (if (empty? part)
       body
       (recur path (inc page) (into body part)))))
  ([path]
   (query-api path 1 [])))

(defn user-path
  [id]
  (str "/users/" id))

(def user-followers (comp #(str % "/followers") user-path))
(def user-shots (comp #(str % "/shots") user-path))

(defn get-user-followers
  [id]
  (->> (user-followers id)
       (query-api)
       (map #(get-in % ["follower" "id"]))))

(defn get-user-shots
  [id]
  (->> (user-shots id)
       (query-api)
       (map #(get % "id"))))

(defn shot-path
  [id]
  (str "/shots/" id))

(def shot-likes (comp #(str % "/likes") shot-path))

(defn get-shot-likes
  [id]
  (->> (shot-likes id)
       (query-api)
       (map #(get-in % ["user" "id"]))))

(defn get-top-ten-likers
  [id]
  (->> (get-user-followers id)
       (mapcat #(get-user-shots %))
       (mapcat #(get-shot-likes %))
       (frequencies)
       (sort-by second)
       (take 10)))
