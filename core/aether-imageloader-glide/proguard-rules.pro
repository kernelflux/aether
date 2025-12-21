-keep @com.kernelflux.fluxrouter.annotation.FluxService class com.kernelflux.aether.imageloader.glide.GlideImageLoader {
    <init>();
}
-keep class com.kernelflux.aether.imageloader.glide.GlideImageLoader implements com.kernelflux.aether.imageloader.api.IImageLoader {
    public <methods>;
}
-dontwarn com.bumptech.glide.**
-keep class com.bumptech.glide.** { *; }
-keep @interface com.kernelflux.fluxrouter.annotation.FluxService { *; }
-keep @com.kernelflux.fluxrouter.annotation.FluxService class * { *; }
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod