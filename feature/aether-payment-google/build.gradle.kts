plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.kernelflux.mobile.androidconfig")
}

android {
    namespace = "com.kernelflux.aether.payment.google"
}

ksp {
    arg("moduleName", "aether-payment-google")
}

dependencies {
    ksp(libs.flux.router.compiler.ksp)
    api(libs.flux.router.core)
    api(project(":feature:aether-payment-api"))
    api(project(":base:aether-common"))
    implementation(project(":base:aether-common"))
    
    // 谷歌支付SDK
    implementation("com.android.billingclient:billing:6.1.0")
    implementation("com.android.billingclient:billing-ktx:6.1.0")
    
    // 协程支持
    implementation(libs.kotlinx.coroutines.core)
}
