-dontwarn com.bumptech.glide.**
-keep class com.bumptech.glide.** { *; }

# Keep network implementation classes - use -keep to prevent removal
-keep class com.kernelflux.aether.network.impl.okhttp.** { *; }
-keep class com.kernelflux.aether.network.okhttp.** { *; }

# Keep converter classes
-keep class com.kernelflux.aether.network.impl.okhttp.converter.** { *; }
-keep class com.kernelflux.aether.network.okhttp.converter.** { *; }

# Keep crypto classes
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.** { *; }
-keep class com.kernelflux.aether.network.okhttp.crypto.** { *; }

# Specifically keep the classes that are missing (with all members)
-keep class com.kernelflux.aether.network.impl.okhttp.converter.DataConverterFactory { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.AesCrypto { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.AesCrypto$Companion { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.RsaCrypto { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.RsaCrypto$Companion { *; }

# Keep factory classes
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.CryptoFactory { *; }

# Keep all classes in impl.okhttp package (prevent removal by R8)
-keep,includedescriptorclasses class com.kernelflux.aether.network.impl.okhttp.** { *; }

# Keep all methods in these classes (especially static factory methods)
-keepclassmembers class com.kernelflux.aether.network.impl.okhttp.converter.DataConverterFactory {
    public static <methods>;
}
-keepclassmembers class com.kernelflux.aether.network.impl.okhttp.crypto.AesCrypto$Companion {
    public static <methods>;
}
-keepclassmembers class com.kernelflux.aether.network.impl.okhttp.crypto.RsaCrypto$Companion {
    public static <methods>;
}
-keepclassmembers class com.kernelflux.aether.network.impl.okhttp.crypto.CryptoFactory {
    public static <methods>;
}

# Keep companion objects (important for static factory methods)
# Use -keep instead of -keepclassmembers to ensure the classes are not removed
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.AesCrypto$Companion { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.RsaCrypto$Companion { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.CryptoFactory { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.converter.DataConverterFactory { *; }

# Keep all companion objects in crypto and converter packages
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.**$Companion { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.converter.**$Companion { *; }

# Keep factory methods (JvmStatic methods)
-keepclassmembers class com.kernelflux.aether.network.impl.okhttp.crypto.AesCrypto {
    public static *** generateKey(...);
    public static *** keyFromPassword(...);
}
-keepclassmembers class com.kernelflux.aether.network.impl.okhttp.crypto.RsaCrypto {
    public static *** generateKeyPair(...);
    public static *** publicKeyFromBytes(...);
    public static *** privateKeyFromBytes(...);
}
-keepclassmembers class com.kernelflux.aether.network.impl.okhttp.crypto.CryptoFactory {
    public static *** create(...);
    public static *** createAesFromPassword(...);
    public static *** createRsaFromBytes(...);
    public static *** createNoOp(...);
}
-keepclassmembers class com.kernelflux.aether.network.impl.okhttp.converter.DataConverterFactory {
    public static *** create(...);
    public static *** createJson(...);
    public static *** createProtobuf(...);
}

# Keep network client implementation
-keep @com.kernelflux.fluxrouter.annotation.FluxService class com.kernelflux.aether.network.okhttp.OkHttpNetworkClient {
    <init>();
}
-keep class com.kernelflux.aether.network.okhttp.OkHttpNetworkClient implements com.kernelflux.aether.network.api.INetworkClient {
    public <methods>;
}

# Protobuf is compileOnly dependency, suppress warnings
-dontwarn com.google.protobuf.**
-dontwarn com.google.protobuf.Message
-dontwarn com.google.protobuf.MessageLite

# Keep attributes
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod