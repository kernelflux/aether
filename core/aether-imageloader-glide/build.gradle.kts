plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.kernelflux.mobile.androidconfig")
    alias(libs.plugins.maven.central.uploader)
}

project.ext.set("publishArtifactId", "aether-imageloader-glide")
project.ext.set("publishVersion", rootProject.ext.get("aetherVersion") as String)

android {
    namespace = "com.kernelflux.aether.imageloader.glide"
}

ksp {
    arg("moduleName", "aether-imageloader-glide")
}

dependencies {
    ksp(libs.flux.router.compiler.ksp)
    api(libs.flux.router.core)
    api(libs.glide)

    debugApi(project(":core:aether-imageloader-api"))
    releaseApi(libs.aether.imageloader.api)
}

apply(from = rootProject.file("gradle/maven-publish.gradle"))
