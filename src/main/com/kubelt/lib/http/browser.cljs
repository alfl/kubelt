(ns com.kubelt.lib.http.browser
  "Support for HTTP requests from a browser execution context."
  {:copyright "©2022 Kubelt, Inc." :license "UNLICENSED"}
  (:require
   [goog.Uri.QueryData :as query]
   [goog.net.XhrIo :as xhrio]
   [goog.structs :as structs])
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :as async :refer [<!]]
   [clojure.string :as str])
  (:require
   [malli.core :as malli]
   [malli.error :as me])
  (:require
   [com.kubelt.lib.error :as lib.error]
   [com.kubelt.lib.http.media-type :as http.media-type]
   [com.kubelt.lib.http.shared :as http.shared]
   [com.kubelt.lib.json :as lib.json]
   [com.kubelt.proto.http :as proto.http]
   [com.kubelt.spec.http :as spec.http]))


(defrecord HttpClient []
  proto.http/HttpClient



  (request!
    [this m]
    (proto.http/request-cb this m #(fn [rtext] rtext)))


  (request-cb
    [this m cb]
    (if-not (malli/validate spec.http/request m)
      ;; The request map is invalid, return a map describing the error.
      (lib.error/explain spec.http/request m)
      ;; We have a valid request map, construct a URL from it before
      ;; making a request.
      (let [
            ;; build url
            method (http.shared/request->method m)
            ;; TODO check method and send post or get
            scheme (http.shared/request->scheme m)
            domain (http.shared/request->domain m)
            port (http.shared/request->port m)
            path (http.shared/request->path m)
            headers (http.shared/request->headers m)
            body (http.shared/request->body m)
            url (str/join "" [scheme "://" domain ":" port path ])]
        ;; The request map is valid, so fire off the request.
        (xhrio/send
         url
         (fn [^js event]                                            
           (let [res (-> event .-target .getResponseText)]          
             (cb res)))                                            
         
         method
         body)))))

(comment
  "request map example"
  {:kubelt/type :kubelt.type/uri
   :http/method method
   :http/version version
   :http/headers headers
   :http/trailers trailers
   :http/status status
   :http/body body
   :uri/scheme scheme
   :uri/port port
   :uri/path path
   :uri/fragment fragment
   :uri/query query
   :uri/domain domain
   :uri/user user}
  )
