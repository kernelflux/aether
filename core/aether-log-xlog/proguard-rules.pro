# ProGuard rules for aether-log-xlog
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep XLogLogger class (FluxService implementation)
-keep @com.kernelflux.fluxrouter.annotation.FluxService class com.kernelflux.aether.log.xlog.XLogLogger {
    <init>();
    public <methods>;
}

# Keep XLogLogger class and all its methods
-keep class com.kernelflux.aether.log.xlog.XLogLogger { *; }
-keep class com.kernelflux.aether.log.xlog.XLogLogger$* { *; }

# Keep Xlog object class and all its methods
# Note: Xlog is a Kotlin object with @JvmStatic methods, no inner classes needed
-keep class com.kernelflux.aether.log.xlog.Xlog { *; }

# Keep native methods (JNI)
-keepclasseswithmembernames class com.kernelflux.aether.log.xlog.Xlog {
    native <methods>;
}

# Keep ILogger interface implementation
-keep class com.kernelflux.aether.log.xlog.XLogLogger implements com.kernelflux.aether.log.api.ILogger {
    public <methods>;
}
