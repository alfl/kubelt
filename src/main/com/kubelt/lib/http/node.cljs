(ns com.kubelt.lib.http.node
  "Support for HTTP requests from a Node.js execution context."
  {:copyright "©2022 Kubelt, Inc." :license "UNLICENSED"}
  (:require
   ["http" :as http :refer [IncomingMessage ServerResponse]]
   ["https" :as https])
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :as async :refer [<!]]
   [clojure.string :as str])
  (:require
   [malli.core :as malli]
   [malli.error :as me]
   [taoensso.timbre :as log])
  (:require
   [com.kubelt.lib.http.media-type :as http.media-type]
   [com.kubelt.lib.http.request :as http.request]
   [com.kubelt.lib.http.shared :as http.shared]
   [com.kubelt.lib.json :as lib.json]
   [com.kubelt.proto.http :as proto.http]
   [com.kubelt.spec.http :as spec.http]))


;; TODO test me
(defn request->node-options
  "Convert a Kubelt HTTP request map into a Node.js http(s) request
  options map."
  [m]
  {:pre [(map? m)]}
  (let [method (http.shared/request->method m)
        domain (http.shared/request->domain m)
        port (http.shared/request->port m)
        path (http.shared/request->path m)
        headers (http.shared/request->headers m)
        body (http.shared/request->body m)
        options {:method method
                 :hostname domain
                 :port port
                 :path path}
        options (cond-> options
                  ;; body
                  (not (nil? body))
                  (assoc :body body)
                  ;; headers
                  (not (nil? headers))
                  (assoc :headers headers))]
    (clj->js options)))

;; Takes a core.async channel on which the result should be placed, and
;; the response object generated by receipt of the upstream response.
(defn on-response
  [c ^ServerResponse res]
  ;; TODO inspect result type
  ;; TODO decode body
  (let [status-code (.-statusCode res)
        headers (.-headers res)
        response {:http/status status-code
                  :http/headers headers}
        body-chan (async/chan)]
    (.on res "data"
         (fn [data]
           (async/go
             (async/>! body-chan data))))
    (.on res "end"
         (fn []
           (async/go
             (async/take! body-chan
                          (fn [body]
                            ;; TODO more generic response type handling
                            ;; TODO give user option of getting JS object, avoiding conversion
                            (let [headers (js->clj headers)
                                  data-edn (cond
                                             (http.media-type/text? headers)
                                             body
                                             (http.media-type/json? headers)
                                             (lib.json/from-json body true)
                                             :else body)]
                              (async/go
                                (async/>! c data-edn)
                                (async/close! c))))))))))

(defn on-error
  [error]
  (log/error error))

;; Public
;; -----------------------------------------------------------------------------
;; TODO support request headers
;; TODO put patch post delete
;; TODO convert response to response map
;; TODO validate response map

(defrecord HttpClient []
  proto.http/HttpClient
  (request!
    [this m]
    (prn {:hereiam "request" :req m})
    (if-not (malli/validate spec.http/request m)
      ;; TODO report an error using common error reporting
      ;; functionality (anomalies).
      (let [explain (-> spec.http/request (malli/explain m) me/humanize)
            error {:com.kubelt/type :kubelt.type/error
                   :error explain}
            response-chan (async/chan)]
        (async/put! response-chan error)
        response-chan)
      ;; The request map is valid, so fire off the request.
      (let [scheme (get m :uri/scheme :http)
            request-map (dissoc m :uri/scheme)
            options (request->node-options request-map)
            ;; Use an unbuffered channel for the response.
            response-chan (async/chan)
            ;; If user specified :https as the request scheme, use the
            ;; Node.js "https" module to fire off the request. Default
            ;; to using the "http" module otherwise.
            request-mod (if (= :https scheme) https http)
            on-response (partial on-response response-chan)
            request (.request request-mod options on-response)]
        ;;(prn options)
        (when (or (http.request/post? m)
                  (http.request/put? m))
          (if-let [data (get m :http/body)]
            (.write request data)))
        (doto request
          (.on "error" on-error)
          (.end))
        ;; Return the channel on which the response will be placed.
        response-chan))))
