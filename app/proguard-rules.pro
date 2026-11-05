-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }

# SLF4J 1.7 discovers its Android logger backend reflectively.
-keep class org.slf4j.impl.StaticLoggerBinder { *; }
