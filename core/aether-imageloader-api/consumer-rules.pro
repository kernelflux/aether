# Aether Image Loader API - Consumer ProGuard Rules
# These rules are automatically applied to consumers of this library
# to prevent obfuscation of public API classes and interfaces

# Keep all public API classes and interfaces
-keep class com.kernelflux.aether.imageloader.api.** { *; }
-keep interface com.kernelflux.aether.imageloader.api.** { *; }

# Keep all enum classes (ImageScaleType, ImagePriority, etc.)
-keepclassmembers enum com.kernelflux.aether.imageloader.api.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
