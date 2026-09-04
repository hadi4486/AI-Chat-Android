# Room generates its implementation classes at compile time; nothing extra needed for it here.

# kotlinx.serialization keeps its own consumer rules, but a couple of explicit keeps make
# reflection-based (de)serialization of our DTOs resilient to aggressive shrinking.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class com.aichat.assistant.data.api.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.aichat.assistant.data.api.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.aichat.assistant.data.api.**$$serializer { *; }
