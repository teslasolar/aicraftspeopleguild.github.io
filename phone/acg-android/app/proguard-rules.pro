# Keep kotlinx.serialization annotations + generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aicraftspeopleguild.acg.**$$serializer { *; }
-keepclassmembers class com.aicraftspeopleguild.acg.** {
    *** Companion;
}
-keepclasseswithmembers class com.aicraftspeopleguild.acg.** {
    kotlinx.serialization.KSerializer serializer(...);
}
