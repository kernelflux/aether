-keep @com.kernelflux.fluxrouter.annotation.FluxService class com.kernelflux.aether.imageloader.glide.GlideImageLoader {
    <init>();
}
-keep class com.kernelflux.aether.imageloader.glide.GlideImageLoader implements com.kernelflux.aether.imageloader.api.IImageLoader {
    public <methods>;
}
-keep @interface com.kernelflux.fluxrouter.annotation.FluxService { *; }
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
