plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.kernelflux.mobile.androidconfig")
    alias(libs.plugins.maven.central.uploader)
}

project.ext.set("publishArtifactId", "aether-utils")
project.ext.set("publishVersion", rootProject.ext.get("aetherVersion") as String)

android {
    namespace = "com.kernelflux.aether.utils"
}

dependencies {
    // 纯工具类模块，不依赖Android Framework
    // 只依赖Kotlin标准库
}

apply(from = rootProject.file("gradle/maven-publish.gradle"))
