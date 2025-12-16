# Keep MMKVStore service for FluxRouter
-keep @com.kernelflux.fluxrouter.annotation.FluxService class com.kernelflux.aether.kv.mmkv.MMKVStore {
    <init>();
}
-keep class com.kernelflux.aether.kv.mmkv.MMKVStore implements com.kernelflux.aether.kv.api.IKVStore {
    public <methods>;
}
-keep @interface com.kernelflux.fluxrouter.annotation.FluxService { *; }
-keep @com.kernelflux.fluxrouter.annotation.FluxService class * { *; }
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep MMKV classes
-keep class com.tencent.mmkv.** { *; }
-dontwarn com.tencent.mmkv.**
