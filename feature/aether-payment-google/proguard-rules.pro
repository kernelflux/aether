# ProGuard rules for aether-payment-google
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes Signature

# 谷歌支付SDK混淆规则
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Keep ResourceHelper class and its methods
-keep class com.kernelflux.aether.common.ResourceHelper { *; }
-dontwarn com.kernelflux.aether.common.ResourceHelper
