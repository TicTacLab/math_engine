(set-env!
 :source-paths #{"src"}
 :repositories {"snapshots" {:url      "http://52.28.244.218:8080/repository/snapshots"
                             :username "admin"
                             :password "NeOpBac8"
                             :snapshots true}
                "releases" {:url      "http://52.28.244.218:8080/repository/internal"
                             :username "admin"
                             :password "NeOpBac8"
                             :releases true}}
 :dependencies '[[org.clojure/clojure "1.6.0"]
                 [malcolmx "0.1.5-SNAPSHOT"]
                 [com.betinvest/noilly "0.1.4"]
                 [org.clojure/tools.trace "0.7.8"]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [ch.qos.logback/logback-core "1.1.3"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.taoensso/nippy "2.9.0"]
                 [com.betinvest/zabbix-clojure-agent "0.1.8"]
                 [clojurewerkz/cassaforte "2.0.2"]
                 [com.stuartsierra/component "0.2.3"]
                 [prismatic/schema "1.0.1"]
                 [cheshire "5.5.0"]
                 [javax.servlet/javax.servlet-api "3.1.0"]
                 [compojure "1.2.0"]
                 [ring "1.4.0"]
                 [dire "0.5.3"]
                 [org.clojure/core.cache "0.6.4"]])

(deftask lc
           "Compile .lc files."
           []
         (fn middleware [next-handler]                      ; [2]
           (fn handler [fileset]
             (clojure.pprint/pprint (output-files fileset))
             (next-handler fileset))))

(deftask build
  "Builds an uberjar of this project that can be run with java -jar"
  []
  (comp
   (aot :all true)
   (pom :project 'malt
        :version "1.0.0")
   (uber :exclude #{#"(?i)^META-INF/INDEX.LIST$"
       #"(?i)^META-INF/[^/]*\.(MF|SF|RSA|DSA)$"
       #"clj$"})
   (show :env true)
   (jar :file "malt-1.0.0.jar"
        :main 'malt.main)))