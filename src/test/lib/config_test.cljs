(ns lib.config-test
  "Test SDK configuration maps."
  (:require
   [cljs.test :as t :refer [deftest is testing use-fixtures]]
   [clojure.string :as str])
  (:require
   [malli.core :as malli])
  (:require
   [com.kubelt.spec.config :as spec.config]
   [com.kubelt.lib.config :as config]))

(deftest config-test
  (testing "default config is valid"
    (let [default-config config/default-config]
      (is (malli/validate spec.config/config default-config)
          "default options must be valid")))

  (testing "logging levels"
    (testing "log level"
      (let [options {:logging/min-level :log}]
        (is (malli/validate spec.config/config options)
            "logging min-level of :log is valid")))
    (testing "trace level"
      (let [options {:logging/min-level :trace}]
        (is (malli/validate spec.config/config options)
            "logging min-level of :trace is valid")))
    (testing "debug level"
      (let [options {:logging/min-level :debug}]
        (is (malli/validate spec.config/config options)
            "logging min-level of :debug is valid")))
    (testing "info level"
      (let [options {:logging/min-level :info}]
        (is (malli/validate spec.config/config options)
            "logging min-level of :info is valid")))
    (testing "warn level"
      (let [options {:logging/min-level :warn}]
        (is (malli/validate spec.config/config options)
            "logging min-level of :warn is valid")))
    (testing "error level"
      (let [options {:logging/min-level :error}]
        (is (malli/validate spec.config/config options)
            "logging min-level of :error is valid")))
    (testing "fatal level"
      (let [options {:logging/min-level :fatal}]
        (is (malli/validate spec.config/config options)
            "logging min-level of :fatal is valid"))))

  (testing "p2p settings"
    (testing "invalid address"
      (let [options {:p2p/read "127.0.0.1"}]
        (is (not (malli/validate spec.config/config options))
            "a dotted-quad is not a valid address"))
      (let [options {:p2p/read "localhost"}]
        (is (not (malli/validate spec.config/config options))
            "localhost is not a valid host name")))

    (testing "multiaddr"
      (testing "localhost"
        (let [options {:p2p/read "/ip4/127.0.0.1"}]
          (is (malli/validate spec.config/config options)
              "loopback IP is a valid network address"))
        (let [options {:p2p/read "/ip4/localhost"}]
          (is (malli/validate spec.config/config options)
              "localhost is a valid network address"))
        (let [options {:p2p/read "/ip4/127.0.0.1/tcp/8080"}]
          (is (malli/validate spec.config/config options)
              "localhost:8080 is a valid network address"))))))
