plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.kernelflux.mobile.androidconfig")
}

android {
    namespace = "com.kernelflux.aether.network.okhttp"
}

ksp {
    arg("moduleName", "aether-network-okhttp")
}

dependencies {
    ksp(libs.flux.router.compiler.ksp)
    api(libs.flux.router.core)
    api(libs.okhttp)
    api(libs.retrofit)
    api(libs.retrofit.gson)
    api(libs.gson)
    api(libs.androidx.core.ktx)

    api(project(":aether-network-api"))

}

