# ProGuard rules for aether-log-api
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep ILogger interface and all its methods
-keep interface com.kernelflux.aether.log.api.ILogger { *; }

# Keep LoggerConfig and all its fields
-keep class com.kernelflux.aether.log.api.LoggerConfig { *; }
-keep class com.kernelflux.aether.log.api.FileConfig { *; }

# Keep LoggerHelper (object class)
# Note: LoggerHelper is a Kotlin object, no inner classes needed
-keep class com.kernelflux.aether.log.api.LoggerHelper { *; }

# Keep LogLevel enum
-keep enum com.kernelflux.aether.log.api.LogLevel { *; }

# Keep AppenderMode enum
-keep enum com.kernelflux.aether.log.api.AppenderMode { *; }

