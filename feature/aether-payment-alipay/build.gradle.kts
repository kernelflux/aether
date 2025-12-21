plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.kernelflux.mobile.androidconfig")
}

android {
    namespace = "com.kernelflux.aether.payment.alipay"
}

ksp {
    arg("moduleName", "aether-payment-alipay")
}

dependencies {
    ksp(libs.flux.router.compiler.ksp)
    api(libs.flux.router.core)
    api(project(":feature:aether-payment-api"))
    api(project(":base:aether-common"))
    
    // 支付宝SDK
    implementation("com.alipay.sdk:alipaysdk-android:15.8.17")
    
    // 协程支持
    implementation(libs.kotlinx.coroutines.core)
}

