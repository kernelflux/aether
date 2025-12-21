plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.kernelflux.mobile.androidconfig")
}

android {
    namespace = "com.kernelflux.aether.payment.wechat"
}

ksp {
    arg("moduleName", "aether-payment-wechat")
}

dependencies {
    ksp(libs.flux.router.compiler.ksp)
    api(libs.flux.router.core)
    api(project(":feature:aether-payment-api"))
    api(project(":base:aether-common"))
    implementation(project(":base:aether-common"))
    
    // 微信支付SDK
    implementation("com.tencent.mm.opensdk:wechat-sdk-android:6.8.0")
}
