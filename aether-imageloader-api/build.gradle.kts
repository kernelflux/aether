plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.kernelflux.mobile.androidconfig")
    alias(libs.plugins.maven.central.uploader)
}

project.ext.set("publishArtifactId", "aether-imageloader-api")
project.ext.set("publishVersion", rootProject.ext.get("aetherVersion") as String)

android {
    namespace = "com.kernelflux.aether.imageloader.api"
}

dependencies {
    //noinspection GradleDependency,UseTomlInstead
    api("androidx.fragment:fragment:1.5.4")
}

// 应用通用发布配置
apply(from = rootProject.file("gradle/maven-publish.gradle"))

