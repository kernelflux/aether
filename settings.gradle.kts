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

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Aether"

// 功能模块 API（接口定义）
include(":aether-imageloader-api")
include(":aether-network-api")
include(":aether-log-api")
include(":aether-kv-api")

// 功能模块实现
include(":aether-imageloader-glide")
include(":aether-network-okhttp")
include(":aether-log-android")
include(":aether-kv-mmkv")

// 业务模块 API（接口定义）
include(":aether-payment-api")
include(":aether-share-api")
include(":aether-login-api")

// 业务模块实现
include(":aether-payment-alipay")
include(":aether-share-wechat")
include(":aether-login-oauth")

// 示例应用
include(":sample")
