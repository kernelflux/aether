plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.kernelflux.mobile.androidconfig")
    alias(libs.plugins.maven.central.uploader)
}

project.ext.set("publishArtifactId", "aether-network-okhttp")
project.ext.set("publishVersion", rootProject.ext.get("aetherVersion") as String)


android {
    namespace = "com.kernelflux.aether.network.okhttp"
}

ksp {
    arg("moduleName", "aether-network-okhttp")
}

dependencies {
    ksp(libs.flux.router.compiler.ksp)
    api(libs.flux.router.core)
    api(libs.okhttp)
    api(libs.retrofit)
    api(libs.retrofit.gson)
    api(libs.gson)
    api(libs.androidx.core.ktx)

    debugApi(project(":core:aether-network-api"))
    releaseApi(libs.aether.network.api)

    // Protobuf 支持（可选，如果项目需要 Protobuf 功能）
    // 注意：如果不需要 Protobuf，可以不添加此依赖
    // 但 ProtobufDataConverter 需要此依赖才能正常工作
    compileOnly(libs.protobuf.java)
}

apply(from = rootProject.file("gradle/maven-publish.gradle"))
