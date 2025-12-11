plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.kernelflux.mobile.androidconfig")
}

android {
    namespace = "com.kernelflux.aether.imageloader.glide"

    defaultConfig {
        // 提供消费者混淆规则
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            // 启用代码压缩和混淆
            // 实现模块需要混淆以保护实现细节
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Debug 版本不混淆，便于调试
            isMinifyEnabled = false
        }
    }

    publishing {
        // 只发布 release 变体到 Maven Central
        singleVariant("release") {}
    }
}

ksp {
    arg("moduleName", "aether-imageloader-glide")
}

dependencies {
    ksp(libs.flux.router.compiler.ksp)
    api(libs.flux.router.core)
    api(project(":aether-imageloader-api"))
    api(libs.glide)

}

