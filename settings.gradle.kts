pluginManagement {
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

// 功能模块 SPI
include(":aether-image-spi")
include(":aether-network-spi")
include(":aether-log-spi")

// 功能模块实现示例
include(":aether-image-impl-glide")
include(":aether-network-impl-okhttp")
include(":aether-log-impl-android")

// 业务模块 SPI
include(":aether-payment-spi")
include(":aether-share-spi")
include(":aether-login-spi")

// 业务模块实现示例
include(":aether-payment-impl-alipay")
include(":aether-share-impl-wechat")
include(":aether-login-impl-oauth")

// 示例应用
include(":sample")
