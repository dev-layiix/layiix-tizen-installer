# Add project specific ProGuard rules here.
# Layiix Tizen Installer: minify disabled in v1, keep file for future release builds.
-keepattributes Signature,InnerClasses,EnclosingMethod

# Apache Santuario (firma XMLDSig .wgt): inizializzazione via risorse/serviceloader.
-keep class org.apache.xml.security.** { *; }
-keep class org.apache.commons.logging.** { *; }
-dontwarn org.apache.commons.logging.**
# StAX/JAXB/Woodstox: usati solo dai rami streaming di Santuario, mai dal nostro path DOM.
-dontwarn javax.xml.stream.**
-dontwarn jakarta.xml.bind.**
-dontwarn com.ctc.wstx.**
-dontwarn org.codehaus.stax2.**
-dontwarn aQute.**
-dontwarn javax.xml.crypto.MarshalException
-dontwarn org.slf4j.impl.StaticLoggerBinder
