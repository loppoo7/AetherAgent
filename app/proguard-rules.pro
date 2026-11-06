-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }

# SLF4J 1.7 discovers its Android logger backend reflectively.
-keep class org.slf4j.impl.StaticLoggerBinder { *; }

# SSHJ's optional EdDSA implementation references a JDK-internal X509Key type.
# That type is not part of Android; EdDSA verification uses the encoded-key path instead.
-dontwarn sun.security.x509.X509Key

# SSHJ dan BouncyCastle dipakai lewat reflection dan service loading; tanpa aturan ini build release gagal.
-keep class net.schmizz.sshj.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn net.schmizz.sshj.**
-dontwarn org.slf4j.**
