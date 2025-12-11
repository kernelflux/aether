# Aether Image Loader Glide Implementation - Consumer ProGuard Rules
# 这些规则会自动应用到使用该库的应用中
# 用于保护公共 API 和必要的实现类不被混淆

# 保护服务注册类（FluxRouter 通过反射查找）
-keep class com.kernelflux.aether.imageloader.glide.GlideImageLoader { *; }

# 保护内部工具类（如果使用者可能通过反射访问）
-keep class com.kernelflux.aether.imageloader.glide.LifecycleSafetyUtils { *; }

# 注意：不要在 consumer-rules.pro 中使用 *Annotation*
# 这会阻止使用该库的应用进行优化
# 如果确实需要运行时注解（如 FluxRouter 的 @FluxService），
# 应该只保留 RuntimeVisibleAnnotations，而不是 *Annotation*

# Glide 库自己处理混淆规则
-dontwarn com.bumptech.glide.**
