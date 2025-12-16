plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.kernelflux.mobile.androidconfig")
    alias(libs.plugins.maven.central.uploader)
}

project.ext.set("publishArtifactId", "aether-kv-mmkv")
project.ext.set("publishVersion", rootProject.ext.get("aetherVersion") as String)

android {
    namespace = "com.kernelflux.aether.kv.mmkv"
}

ksp {
    arg("moduleName", "aether-kv-mmkv")
}

dependencies {
    ksp(libs.flux.router.compiler.ksp)
    api(libs.flux.router.core)
    api(libs.androidx.core.ktx)
    
    debugApi(project(":aether-kv-api"))
    releaseApi(libs.aether.kv.api)
    
    // MMKV 依赖
    api(libs.mmkv)
}

apply(from = rootProject.file("gradle/maven-publish.gradle"))
