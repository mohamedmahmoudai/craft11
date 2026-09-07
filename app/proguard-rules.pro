# ProGuard / R8 rules for Adaptive (FocusCraft)

# Attributes preservation
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Room Database
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
    @androidx.room.TypeConverters *;
}
-keep class com.example.data.local.entity.** { *; }
-keep class com.example.data.local.dao.** { *; }
-keep class com.example.data.local.** { *; }
-dontwarn androidx.room.**

# Domain, Data and UI Models
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.model.** { *; }
-keep class com.example.engine.** { *; }
-keep class com.example.viewmodel.** { *; }
-keep class com.example.data.repository.** { *; }
-keep class com.example.ocr.** { *; }

# Services, Alarms & BroadcastReceivers
-keep class com.example.alarm.** { *; }
-keep class com.example.service.** { *; }
-keep class com.example.ui.screens.SmartAlarmActivity { *; }
-keep class com.example.MainActivity { *; }

# ML Kit Text Recognition
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.mlkit.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# Firebase & Authentication
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Jetpack Compose & Material 3
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.**

# OkHttp and Coroutines
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn kotlinx.coroutines.**

# Kotlinx Serialization
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

