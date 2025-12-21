plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.kernelflux.mobile.androidconfig")
}

android {
    namespace = "com.kernelflux.aether.payment.api"
}

dependencies {
    // 依赖基础模块（ResourceHelper、ResourceKeys）
    api(project(":base:aether-common"))
}

