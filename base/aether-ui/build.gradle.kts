plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.kernelflux.mobile.androidconfig")
    alias(libs.plugins.maven.central.uploader)
}

project.ext.set("publishArtifactId", "aether-ui")
project.ext.set("publishVersion", rootProject.ext.get("aetherVersion") as String)

android {
    namespace = "com.kernelflux.aether.ui"
}

dependencies {
    // 依赖工具类模块
    api(project(":base:aether-common"))
    
    // Android基础依赖
    api(libs.androidx.appcompat)
    api(libs.androidx.fragment)
    api(libs.androidx.activity)
    api(libs.androidx.recyclerview)
    api(libs.androidx.viewpager2)
    api(libs.androidx.coordinatorlayout)
    api(libs.material)
    
    // 协程支持
    api(libs.kotlinx.coroutines.core)
    api(libs.androidx.lifecycle.runtime.ktx)
}

apply(from = rootProject.file("gradle/maven-publish.gradle"))
