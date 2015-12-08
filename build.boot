(require '[clojure.java.shell :as s])

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
 :uberjar-name "malt-engine.jar"

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
                       [org.osgi/org.osgi.core "4.3.0"]
                       [javax.annotation/jsr250-api "1.0"]
                       [javax.enterprise/cdi-api "1.0-SP1"]
                       [javax.inject/javax.inject "1"]])

(defn set-dependencies! []
  (let [project (read-string (slurp "project.clj"))
        deps (->> project
                  (drop 3)
                  (apply hash-map)
                  :dependencies)]
    (set-env! :dependencies deps)))

(set-dependencies!)

(deftask fetch-obfuscating-deps
         "Add some obfuscating dependencies for build"
         []
         (set-env! :dependencies #(into % (get-env :build-dependencies)))
         identity)

(deftask obfuscate
         "Obfuscates jar using KlassMaster"
         []
         (let [uberjar-name (get-env :uberjar-name)
               tmp (tmp-dir!)]
           (with-pre-wrap fileset
             (let [out-file (first (by-name #{uberjar-name} (output-files fileset)))]
               (boot.util/info "Obfuscating %s...\n" uberjar-name)
               (s/sh "java"
                     (str "-Dobfuscator.infile=" (.getAbsolutePath (tmp-file out-file)))
                     (str "-Dobfuscator.outdir=" (.getAbsolutePath tmp))
                     (str "-Dobfuscator.outfile=math-engine.final.jar")
                     "-jar" "obfuscator/ZKM.jar" "obfuscator/script.txt"))

             (-> fileset
                 (add-resource tmp)
                 commit!))))

(deftask build
  "Builds an uberjar of this project that can be run with java -jar"
  []
  (comp
    (aot :all true)
    (pom :project 'malt-engine
         :version "1.0.0")
    (uber :exclude #{#"(?i)^META-INF/INDEX.LIST$"
                     #"(?i)^META-INF/[^/]*\.(MF|SF|RSA|DSA)$"
                     #"clj$"
                     ;; zellix klassmaster breaks when class hash DASH in his name
                     #"Compressable-LZMA2"})
    (jar :file (get-env :uberjar-name)
         :main 'malt.main)
    (obfuscate)))