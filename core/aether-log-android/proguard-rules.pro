# ProGuard rules for aether-log-android
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep AndroidLogLogger class (FluxService implementation)
-keep @com.kernelflux.fluxrouter.annotation.FluxService class com.kernelflux.aether.log.android.AndroidLogLogger {
    <init>();
    public <methods>;
}

# Keep AndroidLogLogger class and all its methods
-keep class com.kernelflux.aether.log.android.AndroidLogLogger { *; }
-keep class com.kernelflux.aether.log.android.AndroidLogLogger$* { *; }

# Keep ILogger interface implementation
-keep class com.kernelflux.aether.log.android.AndroidLogLogger implements com.kernelflux.aether.log.api.ILogger {
    public <methods>;
}

