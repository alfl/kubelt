(ns build.command
  "Miscellaneous build-related tooling."
  {:copyright "©2021 Kubelt, Inc." :license "UNLICENSED"}
  (:require
   [clojure.tools.cli :as cli])
  (:require
   [shadow.cljs.devtools.api :as shadow]))

(def example-options
  [["-o" "--out-file NAME" "Output file name"
    :default "target/example.txt"]])

(defn example
  "Just an example command that can be invoked by shadow-cljs."
  [& args]
  (shadow/compile :sdk)
  (let [{:keys [options] :as opts} (cli/parse-opts args example-options)
        {:keys [out-file]} options]
    (spit out-file "lorem ipsum")))
