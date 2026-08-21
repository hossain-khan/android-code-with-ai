# ==================================================================================
# ProGuard & R8 Optimization Rules for CodeMateX
# ==================================================================================

# Preserve source file names and line numbers for readable production stacktraces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature

# ----------------------------------------------------------------------------------
# 1. Native JNI & Google LiteRT-LM (On-Device AI Inference Engine)
# Reference: https://developer.android.com/ndk/guides/
# ----------------------------------------------------------------------------------
# Preserve all JNI native entry points
-keepclasseswithmembernames class * {
    native <methods>;
}

# Retain LiteRT-LM classes and callbacks called via JNI from liblitertlm_jni.so
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# ----------------------------------------------------------------------------------
# 2. AndroidX Room Persistence
# Reference: https://developer.android.com/training/data-storage/room
# ----------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ----------------------------------------------------------------------------------
# 3. Slack Circuit UDF Framework
# Reference: https://slackhq.github.io/circuit/
# ----------------------------------------------------------------------------------
-dontwarn com.slack.circuit.**
-keep class com.slack.circuit.** { *; }

# ----------------------------------------------------------------------------------
# 4. Dependency Injection (Metro DI & javax.inject)
# ----------------------------------------------------------------------------------
-dontwarn dev.zacsweers.metro.**
-keep class dev.zacsweers.metro.** { *; }
-keepclassmembers class * {
    @dev.zacsweers.metro.Inject <init>(...);
    @javax.inject.Inject <init>(...);
}

# ----------------------------------------------------------------------------------
# 5. KotlinX Serialization & Retrofit
# ----------------------------------------------------------------------------------
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# ----------------------------------------------------------------------------------
# 6. OkHttp / Retrofit Networking
# ----------------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
