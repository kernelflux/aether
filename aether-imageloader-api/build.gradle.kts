import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.central.uploader)
}

// 发布配置
project.ext.set("publishArtifactId", "aether-imageloader-api")
project.ext.set("publishVersion", rootProject.ext.get("aetherVersion") as String)

android {
    namespace = "com.kernelflux.aether.imageloader.api"

    compileSdk = 36
    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            // API 模块主要是接口定义，通常不需要混淆
            // 如果未来有内部实现需要混淆，再启用
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }


    buildFeatures {
        buildConfig = true
    }

    publishing {
        singleVariant("release") {}
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        // 跳过元数据版本检查，因为 OkHttp 可能使用较新的 Kotlin 版本编译
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

dependencies {
    //noinspection GradleDependency,UseTomlInstead
    api("androidx.fragment:fragment:1.5.4")
}

// 应用通用发布配置
apply(from = rootProject.file("gradle/maven-publish.gradle"))

