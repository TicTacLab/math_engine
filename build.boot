(set-env!
 :source-paths #{"src"}
 :repositories {"snapshots" {:url      "http://52.28.244.218:8080/repository/snapshots"
                             :username "admin"
                             :password "NeOpBac8"
                             :snapshots true
                             :releases false}
                "releases" {:url      "http://52.28.244.218:8080/repository/internal"
                             :username "admin"
                             :password "NeOpBac8"
                             :releases true
                            :snapshots false}
                "central" "http://repo1.maven.org/maven2/"}
 :dependencies '[[org.clojure/clojure "1.8.0-RC3"]
                 [malcolmx "0.1.5"]
                 [com.betinvest/noilly "0.1.4"]
                 [org.clojure/tools.trace "0.7.8"]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [ch.qos.logback/logback-core "1.1.3"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.taoensso/nippy "2.9.0"]
                 [metrics-clojure "2.4.0"]
                 ;[com.betinvest/zabbix-clojure-agent "0.1.8"]
                 [clojurewerkz/cassaforte "2.0.2"]
                 [com.stuartsierra/component "0.2.3"]
                 [prismatic/schema "1.0.1"]
                 [cheshire "5.5.0"]
                 [javax.servlet/javax.servlet-api "3.1.0"]
                 [compojure "1.4.0"]
                 [ring "1.4.0"]
                 [dire "0.5.3"]
                 [org.clojure/core.cache "0.6.4"]]
 :build-dependencies '[[org.codehaus.groovy/groovy-all "2.4.5"]
                       [javax.mail/javax.mail-api "1.5.4"]
                       [javax.jms/jms-api "1.1-rev-1"]
                       [org.codehaus.janino/janino "2.7.8"]
                       [org.codehaus.janino/commons-compiler "2.6.1"]
                       [org.xerial.snappy/snappy-java "1.1.2"]
                       [org.hdrhistogram/HdrHistogram "2.1.7"]
                       [org.apache.poi/poi-ooxml-schemas "3.13"]
                       [org.apache.xmlbeans/xmlbeans "2.6.0"]
                       [org.apache.poi/ooxml-security "1.0"]
                       [com.jcraft/jzlib "1.1.3"]
                       [org.jboss.marshalling/jboss-marshalling "1.4.10.Final"]
                       [org.eclipse.jetty.alpn/alpn-api "1.1.2.v20150522"]
                       [org.eclipse.jetty.npn/npn-api "1.1.1.v20141010"]
                       [org.apache.tomcat/tomcat-jni "9.0.0.M1"]
                       [org.bouncycastle/bcprov-jdk15on "1.51"]
                       [org.bouncycastle/bcpkix-jdk15on "1.51"]
                       [javassist/javassist "3.12.1.GA"]
                       [commons-logging/commons-logging "1.2"]
                       [log4j/log4j "1.2.17"]
                       [javax.xml/jaxp-api "1.4.1"]
                       [org.apache.santuario/xmlsec "1.5.1"]
                       [javax.xml.stream/stax-api "1.0-2"]
                       [junit/junit "4.12"]
                       [org.apache.poi/ooxml-schemas "1.1"]
                       [org.apache.ant/ant "1.9.6"]
                       [org.eclipse.jetty/jetty-jmx "9.3.6.v20151106"]
                       [org.apache.hadoop/hadoop-core "1.2.1"]
                       [org.jboss.logging/jboss-logging "3.3.0.Final"]
                       [org.osgi/org.osgi.enterprise "4.2.0"]
                       [javax.portlet/portlet-api "2.0"]
                       [org.osgi/org.osgi.core "4.3.0"]])

(deftask prod "Add some prod dependencies for build"
         []
         (set-env! :dependencies #(into % (get-env :build-dependencies)))
         identity)

(deftask build
  "Builds an uberjar of this project that can be run with java -jar"
  []
  (comp
    (prod)
    (aot :all true)
    (pom :project 'malt
         :version "1.0.0")
    (uber :exclude #{#"(?i)^META-INF/INDEX.LIST$"
                     #"(?i)^META-INF/[^/]*\.(MF|SF|RSA|DSA)$"
                     #"clj$"})
    (jar :file "malt-1.0.0.jar"
         :main 'malt.main)))