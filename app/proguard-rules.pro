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

# Retain LiteRT-LM classes, configs (SamplerConfig, ThinkingConfig, etc.), and callbacks called via JNI from liblitertlm_jni.so
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

