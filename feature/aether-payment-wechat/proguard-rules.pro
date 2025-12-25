# ProGuard rules for aether-payment-wechat
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes Signature

# 微信支付SDK混淆规则
-keep class com.tencent.mm.opensdk.** { *; }
-keep class com.tencent.wxop.** { *; }
-keep class com.tencent.mm.sdk.** { *; }

# Keep ResourceHelper class and its methods
-keep class com.kernelflux.aether.payment.api.ResourceHelper { *; }
-dontwarn com.kernelflux.aether.payment.api.ResourceHelper
