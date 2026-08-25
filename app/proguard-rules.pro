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

# Preserve JNI callback interface invoked from native LiteRT-LM C++ background threads
-keep class com.google.ai.edge.litertlm.MessageCallback { *; }
-dontwarn com.google.ai.edge.litertlm.**

