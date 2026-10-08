# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.saathi.assistant.**$$serializer { *; }
-keepclassmembers class com.saathi.assistant.** { *** Companion; }
-keepclasseswithmembers class com.saathi.assistant.** { kotlinx.serialization.KSerializer serializer(...); }
