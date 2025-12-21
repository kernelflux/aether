plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.kernelflux.mobile.androidconfig")
    alias(libs.plugins.maven.central.uploader)
}

project.ext.set("publishArtifactId", "aether-common")
project.ext.set("publishVersion", rootProject.ext.get("aetherVersion") as String)

android {
    namespace = "com.kernelflux.aether.common"
}

dependencies {
    // 依赖工具类模块
    api(project(":base:aether-utils"))
}

apply(from = rootProject.file("gradle/maven-publish.gradle"))
