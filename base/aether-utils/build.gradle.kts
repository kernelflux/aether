plugins {
    kotlin("jvm")
    alias(libs.plugins.maven.central.uploader)
}

project.ext.set("publishArtifactId", "aether-utils")
project.ext.set("publishVersion", rootProject.ext.get("aetherVersion") as String)

kotlin {
    jvmToolchain(11)
    
    compilerOptions {
        // 跳过元数据版本检查
        freeCompilerArgs.add("-Xskip-metadata-version-check")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // 纯工具类模块，不依赖Android Framework
    // 只依赖Kotlin标准库
}

apply(from = rootProject.file("gradle/maven-publish.gradle"))
