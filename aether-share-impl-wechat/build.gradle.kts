plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.kernelflux.mobile.androidconfig")
}

android {
    namespace = "com.kernelflux.aether.share.impl.wechat"
}

ksp {
    arg("moduleName", "aether-share-impl-wechat")
}

dependencies {
    ksp(libs.flux.router.compiler.ksp)
    api(libs.flux.router.core)
    api(project(":aether-share-spi"))
}

