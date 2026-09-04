# ProGuard / R8 rules for FocusCraft

# Room database entities, DAOs, and Database classes
-keep class com.example.data.local.entity.** { *; }
-keep class com.example.data.local.dao.** { *; }
-keep class com.example.data.local.** { *; }

# Domain and UI Models
-keep class com.example.model.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Google Generative AI (Gemini)
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**
