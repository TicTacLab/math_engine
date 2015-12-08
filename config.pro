-injars       target/malt-1.0.0.jar
-outjars      target/malt-engine-1.0.0.jar

-libraryjars  "<java.home>/lib/resources.jar"
-libraryjars  "<java.home>/lib/rt.jar"
-libraryjars  "<java.home>/lib/jsse.jar"
-libraryjars  "<java.home>/lib/jce.jar"
-libraryjars  "<java.home>/lib/charsets.jar"
-libraryjars  "<java.home>/lib/jfr.jar"
-libraryjars  "<java.home>/../lib/tools.jar"
-libraryjars  "<user.home>/.m2/repository/org/codehaus/groovy/groovy-all/2.4.5/groovy-all-2.4.5.jar"
-libraryjars  "<user.home>/.m2/repository/javax/mail/javax.mail-api/1.5.4/javax.mail-api-1.5.4.jar"
-libraryjars  "<user.home>/.m2/repository/javax/jms/jms-api/1.1-rev-1/jms-api-1.1-rev-1.jar"
-libraryjars  "<user.home>/.m2/repository/org/codehaus/janino/janino/2.7.8/janino-2.7.8.jar"
-libraryjars  "<user.home>/.m2/repository/org/codehaus/janino/commons-compiler/2.6.1/commons-compiler-2.6.1.jar"
-libraryjars  "<user.home>/.m2/repository/org/xerial/snappy/snappy-java/1.1.2/snappy-java-1.1.2.jar"
-libraryjars  "<user.home>/.m2/repository/org/hdrhistogram/HdrHistogram/2.1.7/HdrHistogram-2.1.7.jar"
-libraryjars  "<user.home>/.m2/repository/org/apache/poi/poi-ooxml-schemas/3.13/poi-ooxml-schemas-3.13.jar"
-libraryjars  "<user.home>/.m2/repository/org/apache/xmlbeans/xmlbeans/2.6.0/xmlbeans-2.6.0.jar"
-libraryjars  "<user.home>/.m2/repository/org/apache/poi/ooxml-security/1.0/ooxml-security-1.0.jar"
-libraryjars  "<user.home>/.m2/repository/com/jcraft/jzlib/1.1.3/jzlib-1.1.3.jar"
-libraryjars  "<user.home>/.m2/repository/org/jboss/marshalling/jboss-marshalling/1.4.10.Final/jboss-marshalling-1.4.10.Final.jar"
-libraryjars  "<user.home>/.m2/repository/org/eclipse/jetty/alpn/alpn-api/1.1.2.v20150522/alpn-api-1.1.2.v20150522.jar"
-libraryjars  "<user.home>/.m2/repository/org/eclipse/jetty/npn/npn-api/1.1.1.v20141010/npn-api-1.1.1.v20141010.jar"
-libraryjars  "<user.home>/.m2/repository/org/apache/tomcat/tomcat-jni/9.0.0.M1/tomcat-jni-9.0.0.M1.jar"
-libraryjars  "<user.home>/.m2/repository/org/bouncycastle/bcprov-jdk15on/1.51/bcprov-jdk15on-1.51.jar"
-libraryjars  "<user.home>/.m2/repository/org/bouncycastle/bcpkix-jdk15on/1.51/bcpkix-jdk15on-1.51.jar"
-libraryjars  "<user.home>/.m2/repository/javassist/javassist/3.12.1.GA/javassist-3.12.1.GA.jar"
-libraryjars  "<user.home>/.m2/repository/commons-logging/commons-logging/1.2/commons-logging-1.2.jar"
-libraryjars  "<user.home>/.m2/repository/log4j/log4j/1.2.17/log4j-1.2.17.jar"
-libraryjars  "<user.home>/.m2/repository/javax/xml/jaxp-api/1.4.1/jaxp-api-1.4.1.jar"
-libraryjars  "<user.home>/.m2/repository/org/apache/santuario/xmlsec/1.5.1/xmlsec-1.5.1.jar"
-libraryjars  "<user.home>/.m2/repository/javax/xml/stream/stax-api/1.0-2/stax-api-1.0-2.jar"
-libraryjars  "<user.home>/.m2/repository/junit/junit/4.12/junit-4.12.jar"
-libraryjars  "<user.home>/.m2/repository/org/apache/poi/ooxml-schemas/1.1/ooxml-schemas-1.1.jar"
-libraryjars  "<user.home>/.m2/repository/org/apache/ant/ant/1.9.6/ant-1.9.6.jar"
-libraryjars  "<user.home>/.m2/repository/org/eclipse/jetty/jetty-jmx/9.3.6.v20151106/jetty-jmx-9.3.6.v20151106.jar"
-libraryjars  "<user.home>/.m2/repository/org/apache/hadoop/hadoop-core/1.2.1/hadoop-core-1.2.1.jar"
-libraryjars  "<user.home>/.m2/repository/org/jboss/logging/jboss-logging/3.3.0.Final/jboss-logging-3.3.0.Final.jar"
-libraryjars  "<user.home>/.m2/repository/org/osgi/org.osgi.enterprise/4.2.0/org.osgi.enterprise-4.2.0.jar"
-libraryjars  "<user.home>/.m2/repository/javax/portlet/portlet-api/2.0/portlet-api-2.0.jar"
-libraryjars  "<user.home>/.m2/repository/org/osgi/org.osgi.core/4.3.0/org.osgi.core-4.3.0.jar"

-printmapping myapplication.map

-keep public class malt.main {
    public static void main(java.lang.String[]);
}

-dontskipnonpubliclibraryclassmembers

-keep class clojure.** { *; }
-keep class com.datastax.** { *; }
-keep class io.netty.** { *; }
-keep class javax.servlet.** { *; }
-keep class **.*__init { *; }
-keep class net.jpountz.lz4.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class schemasMicrosoftComVml.** { *; }
-keep class schemasMicrosoftComOfficeOffice.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class * extends org.apache.poi.POIXMLRelation { *; }
-keep class com.google.common.** { *; }
-keep class ch.qos.logback.** { *; }
-keep class com.stuartsierra.dependency.DependencyGraph { *; }
-keep class com.stuartsierra.component.Lifecycle { *; }
-keep class * implements clojure.lang.IPersistentMap { *; }

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

-forceprocessing

-dontwarn
-dontnote
-dontoptimize