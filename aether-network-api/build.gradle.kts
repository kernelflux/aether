plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.kernelflux.mobile.androidconfig")
}

android {
    namespace = "com.kernelflux.aether.network.api"
}

dependencies {
    // Kotlin 协程支持（Flow 需要）
    api(libs.kotlinx.coroutines.core)
}

