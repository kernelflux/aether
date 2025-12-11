# Aether Image Loader Glide Implementation - Internal ProGuard Rules
# These rules apply to this library's own code during build
# 用于混淆实现模块的内部代码，保护必要的类不被混淆

# 必须保持：FluxRouter 服务注册类（通过反射注册）
-keep class com.kernelflux.aether.imageloader.glide.GlideImageLoader { *; }

# 必须保持：内部工具类（如果通过反射使用）
-keep class com.kernelflux.aether.imageloader.glide.LifecycleSafetyUtils { *; }

# Glide 库自己处理混淆规则，我们只需要不警告
-dontwarn com.bumptech.glide.**

# 保持必要的属性（用于库自身构建）
# 注意：*Annotation* 只在库自己的 proguard-rules.pro 中使用
# 不要放在 consumer-rules.pro 中，否则会阻止应用优化
-keepattributes *Annotation*,SourceFile,LineNumberTable,Signature,Exceptions

# 其他类可以混淆（减少体积，保护实现细节）
# 注意：公共 API 通过 consumer-rules.pro 保护
