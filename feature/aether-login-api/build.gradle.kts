plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.kernelflux.mobile.androidconfig")
}

android {
    namespace = "com.kernelflux.aether.login.api"
}

dependencies {
    // 使用Android标准Resources系统，不需要额外依赖
}

