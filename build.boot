(set-env!
  :resource-paths #{"resources" "src"}
  :dependencies '[[org.clojure/clojure "1.5.1"]
                  [com.betinvest/poi "3.9.1"]
                  [org.apache.poi/poi-ooxml "3.9"]
                  [org.apache.poi/poi-ooxml-schemas "3.9"]
                  [commons-codec/commons-codec "1.9"]
                  [org.apache.xmlbeans/xmlbeans "2.6.0"]
                  [dom4j/dom4j "1.6.1"]
                  [org.apache.commons/commons-math3 "3.1.1"]
                  [org.clojure/tools.trace "0.7.5"]
                  [org.clojure/tools.cli "0.2.2"]
                  [org.slf4j/slf4j-api "1.7.7"]
                  [ch.qos.logback/logback-core "1.1.2"]
                  [ch.qos.logback/logback-classic "1.1.2"]
                  [org.clojure/tools.logging "0.3.1"]
                  [org.flatland/protobuf "0.8.1"]
                  [com.google.protobuf/protobuf-java "2.5.0"]
                  [com.taoensso/nippy "2.5.2"]
                  [com.betinvest/zabbix-clojure-agent "0.1.8"]
                  [clojurewerkz/cassaforte "2.0.0"]
                  [environ "1.0.0"]
                  [com.stuartsierra/component "0.2.2"]
                  [prismatic/schema "0.3.7"]
                  [cheshire "5.4.0"]
                  [javax.servlet/javax.servlet-api "3.1.0"]
                  [compojure "1.2.0"]
                  [ring "1.3.2"]
                  [org.clojure/core.cache "0.6.4"]]
  :repositories {"nassau" {:url      "http://nassau/repository/internal"
                           :username "ci"
                           :password "ci1"}})
(task-options!
  pom  {:project 'malt_engine}
  aot  {:all true}
  uber {:as-jars true}
  jar  {:main 'malt.main}
  push {:tag            true
        :ensure-release true
        :repo           "nassau"})

(deftask
  protoc
  []
  (let [tmp (tmp-dir!)]
    (with-pre-wrap fileset
                   (prn (->> (input-files fileset)
                             (map tmp-path)
                             (filter #(.endsWith % ".clj"))
                             (map boot.util/path->ns)))
                   (empty-dir! tmp)
                   (doseq [f (->> fileset
                                  (input-files)
                                  (by-ext [".proto"])
                                  (map boot.tmpdir/file))]
                     (boot.util/info "Compiling protobuf file: %s\n" (.getName f))
                     (let [res (clojure.java.shell/sh "/usr/bin/protoc"
                                                      (str "--proto_path=" (.getParent f))
                                                      (str "--java_out=" (.getPath tmp))
                                                      (.getPath f))]
                       (when-not (zero? (:exit res))
                         (boot.util/fail "stdout: %s\n" (:out res))
                         (boot.util/fail "stderr: %s\n" (:err res)))))
                   (-> fileset
                       (add-source (clojure.java.io/file tmp))
                       (commit!)))))

(deftask
  release
  [v version VER    str    "The new project version"]
  (comp (pom :version version)
        (protoc)
        (javac)
        (aot)
        (uber)
        (jar)
        #_(push :file-regex #{(re-pattern (format "malt_engine-%s.jar$" version))})))