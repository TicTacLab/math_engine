(defproject malt "1.0.0-SNAPSHOT"
  :description "REST FOR CAST EXCEL"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.1"]
                 [malt.poi/poi "3.9.1"]
                 [org.apache.poi/poi-ooxml "3.9"]
                 [org.apache.poi/poi-ooxml-schemas "3.9"]
                 [commons-codec/commons-codec "1.9"]
                 [org.apache.xmlbeans/xmlbeans "2.6.0"]
                 [dom4j/dom4j "1.6.1"]
                 [org.apache.commons/commons-math3 "3.1.1"]
                 [overtone/at-at "1.1.1"]
                 [org.clojure/tools.trace "0.7.5"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.slf4j/slf4j-api "1.7.7"]
                 [ch.qos.logback/logback-core "1.1.2"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.taoensso/timbre "2.6.2"]
                 [clj-pid "0.1.1"]
                 [org.flatland/protobuf "0.8.1"]
                 [com.google.protobuf/protobuf-java "2.5.0"]
                 [com.hazelcast/hazelcast-client "3.1.5"]
                 [com.taoensso/nippy "2.5.2"]
                 [com.betinvest/zabbix-clojure-agent "0.1.5"]
                 [clojurewerkz/cassaforte "2.0.0"]
                 [environ "1.0.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [prismatic/schema "0.3.7"]
                 [cheshire "5.4.0"]
                 [javax.servlet/javax.servlet-api "3.1.0"]
                 [compojure "1.2.0"]
                 [ring "1.3.2"]
                 [org.clojure/core.cache "0.6.4"]]

  :source-paths ["src"]

  :repl-options {:timeout 120000
                 :init-ns user}
  :uberjar-name "malt-standalone.jar"
  :jvm-opts ["-Dlogback.configurationFile=logback.xml"]
  :repositories ^:replace [
["snapshots" {:url "http://10.1.4.197:8080/repository/snapshots"
                               :username :env
                               :password :env}]
["releases" {:url "http://10.1.4.197:8080/repository/internal"
                               :username :env
                               :password :env}]]

  :plugins [[org.apache.maven.wagon/wagon-ssh-external "2.6"]
            [lein-ring "0.8.2"]
            [lein-protobuf "0.4.1"]
            [lein-environ "1.0.0"]]

  :profiles {
             :dev [:dev-env
                   {:source-paths ["dev"]
                    :dependencies [[ns-tracker "0.2.2"]
                                   [aprint "0.1.0"]
                                   [http-kit.fake "0.2.1"]]}]
             :test [:test-env]}
  :main malt.main)

(cemerick.pomegranate.aether/register-wagon-factory!
 "scp" #(let [c (resolve 'org.apache.maven.wagon.providers.ssh.external.ScpExternalWagon)]
          (clojure.lang.Reflector/invokeConstructor c (into-array []))))
