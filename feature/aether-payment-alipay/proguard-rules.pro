# ProGuard rules for aether-payment-alipay
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes Signature

# Keep ResourceHelper class and its methods
-keep class com.kernelflux.aether.common.ResourceHelper { *; }
-dontwarn com.kernelflux.aether.common.ResourceHelper
