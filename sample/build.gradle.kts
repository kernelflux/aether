plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.flux.router)
    id("com.kernelflux.mobile.androidconfig")
}

dependencies {
    implementation(libs.flux.router.core)
    implementation(libs.recyclerview)
    ksp(libs.flux.router.compiler.ksp)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)

    // 功能模块 API
    implementation(project(":aether-imageloader-api"))
    implementation(project(":aether-network-api"))
    implementation(project(":aether-log-api"))
    
    // 功能模块实现
    implementation(project(":aether-imageloader-glide"))
    implementation(project(":aether-network-okhttp"))
    implementation(project(":aether-log-android"))
    
    // 业务模块 API
    implementation(project(":aether-payment-api"))
    implementation(project(":aether-share-api"))
    implementation(project(":aether-login-api"))
    
    // 业务模块实现
    implementation(project(":aether-payment-alipay"))
    implementation(project(":aether-share-wechat"))
    implementation(project(":aether-login-oauth"))
    
    // Glide KSP annotation processor
    ksp(libs.glide.ksp)
}

ksp {
    arg("moduleName", "sample")
}