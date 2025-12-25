pluginManagement {
    // 包含插件构建（必须在 repositories 之前）
    includeBuild("appconfig")
    
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Aether"

// ==================== 基础模块组（Base Group）====================
include(":base:aether-utils")        // 纯工具类（无Android依赖）
include(":base:aether-ui")            // UI基础组件
include(":base:aether-common")        // 通用工具（Android相关）

// ==================== 核心模块组（Core Group）====================
// 图片加载
include(":core:aether-imageloader-api")
include(":core:aether-imageloader-glide")

// 网络
include(":core:aether-network-api")
include(":core:aether-network-okhttp")

// 日志
include(":core:aether-log-api")
include(":core:aether-log-xlog")
include(":core:aether-log-android")

// 键值存储
include(":core:aether-kv-api")
include(":core:aether-kv-mmkv")

// ==================== 功能模块组（Feature Group）====================
// 支付
include(":feature:aether-payment-api")
include(":feature:aether-payment-alipay")
include(":feature:aether-payment-wechat")
include(":feature:aether-payment-google")

// 分享
include(":feature:aether-share-api")
include(":feature:aether-share-wechat")

// 登录
include(":feature:aether-login-api")
include(":feature:aether-login-oauth")

// ==================== 示例应用 ====================
include(":sample")
