plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.flux.router)
    id("com.kernelflux.mobile.androidconfig")
}

dependencies {
    implementation(libs.flux.router.core)
    ksp(libs.flux.router.compiler.ksp)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // 功能模块SPI
    implementation(project(":aether-image-spi"))
    implementation(project(":aether-network-spi"))
    implementation(project(":aether-log-spi"))
    
    // 功能模块实现
    implementation(project(":aether-image-impl-glide"))
    implementation(project(":aether-network-impl-okhttp"))
    implementation(project(":aether-log-impl-android"))
    
    // 业务模块SPI
    implementation(project(":aether-payment-spi"))
    implementation(project(":aether-share-spi"))
    implementation(project(":aether-login-spi"))
    
    // 业务模块实现
    implementation(project(":aether-payment-impl-alipay"))
    implementation(project(":aether-share-impl-wechat"))
    implementation(project(":aether-login-impl-oauth"))
}

ksp {
    arg("moduleName", "sample")
}