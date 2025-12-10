plugins {
    `kotlin-dsl` // 让 Gradle 识别这个目录为插件工程
}


gradlePlugin {
    plugins {
        val androidConfigPlugin = this.create("androidConfig")
        androidConfigPlugin.id = "com.kernelflux.mobile.androidconfig" // 插件 ID
        androidConfigPlugin.implementationClass =
            "com.kernelflux.mobile.androidconfig.AndroidConfigPlugin" // 插件的实现类
    }
}

dependencies {
    //noinspection UseTomlInstead
    implementation("com.android.tools.build:gradle:8.13.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
}

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