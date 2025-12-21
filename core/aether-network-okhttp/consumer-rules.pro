# Consumer ProGuard rules for aether-network-okhttp
# These rules are automatically applied to consumers of this library

# Keep network implementation classes
-keep class com.kernelflux.aether.network.okhttp.** { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.** { *; }

# Keep converter classes
-keep class com.kernelflux.aether.network.okhttp.converter.** { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.converter.** { *; }

# Keep crypto classes
-keep class com.kernelflux.aether.network.okhttp.crypto.** { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.** { *; }

# Keep companion objects and factory methods (important for static methods)
# Use -keep instead of -keepclassmembers to ensure classes are not removed
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.AesCrypto$Companion { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.RsaCrypto$Companion { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.CryptoFactory { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.converter.DataConverterFactory { *; }

# Keep all companion objects
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.**$Companion { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.converter.**$Companion { *; }

# Keep network client implementation
-keep @com.kernelflux.fluxrouter.annotation.FluxService class com.kernelflux.aether.network.okhttp.OkHttpNetworkClient {
    <init>();
}
-keep class com.kernelflux.aether.network.okhttp.OkHttpNetworkClient implements com.kernelflux.aether.network.api.INetworkClient {
    public <methods>;
}

# Keep network state manager
-keep class com.kernelflux.aether.network.okhttp.DefaultNetworkStateManager { *; }
-keep class com.kernelflux.aether.network.okhttp.NetworkStateManagerFactory { *; }

# Keep cookie jar
-keep class com.kernelflux.aether.network.okhttp.PersistentCookieJar { *; }

# Keep attributes
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
