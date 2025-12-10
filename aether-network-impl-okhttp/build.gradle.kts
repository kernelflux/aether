plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.kernelflux.mobile.androidconfig")
}

android {
    namespace = "com.kernelflux.aether.network.impl.okhttp"
}

ksp {
    arg("moduleName", "aether-network-impl-okhttp")
}

dependencies {
    ksp(libs.flux.router.compiler.ksp)
    api(libs.flux.router.core)
    api(project(":aether-network-spi"))
    api(libs.okhttp)
    api(libs.retrofit)
    api(libs.retrofit.gson)
    api(libs.gson)
}

