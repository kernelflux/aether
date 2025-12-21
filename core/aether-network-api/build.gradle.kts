plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.kernelflux.mobile.androidconfig")
    alias(libs.plugins.maven.central.uploader)
}


project.ext.set("publishArtifactId", "aether-network-api")
project.ext.set("publishVersion", rootProject.ext.get("aetherVersion") as String)

android {
    namespace = "com.kernelflux.aether.network.api"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    // 使用Android标准Resources系统，不需要额外依赖
}

apply(from = rootProject.file("gradle/maven-publish.gradle"))

