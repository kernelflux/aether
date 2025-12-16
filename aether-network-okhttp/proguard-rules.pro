# ProGuard rules for aether-network-okhttp library itself
# Used when this library is minified

# Keep all public API classes
-keep class com.kernelflux.aether.network.okhttp.** { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.** { *; }

# Keep converter classes
-keep class com.kernelflux.aether.network.impl.okhttp.converter.** { *; }

# Keep crypto classes
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.** { *; }

# Keep companion objects (important for static factory methods)
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.AesCrypto$Companion { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.RsaCrypto$Companion { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.crypto.CryptoFactory { *; }
-keep class com.kernelflux.aether.network.impl.okhttp.converter.DataConverterFactory { *; }

# Keep attributes
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
