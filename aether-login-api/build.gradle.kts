plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.kernelflux.mobile.androidconfig")
}

android {
    namespace = "com.kernelflux.aether.login.api"
}

dependencies {
    // 接口层不依赖core，保持纯净
}

