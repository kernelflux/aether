package com.kernelflux.mobile.androidconfig

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class AndroidConfigPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 统一配置 kotlinOptions
        project.afterEvaluate {
            tasks.withType(KotlinJvmCompile::class.java).configureEach {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
                // 跳过元数据版本检查，因为 OkHttp 可能使用较新的 Kotlin 版本编译
                compilerOptions.freeCompilerArgs.add("-Xskip-metadata-version-check")
            }
        }

        // 兼容 com.android.library 模块
        project.plugins.withId("com.android.library") {
            project.extensions.configure<LibraryExtension> {
                compileSdk = 36
                defaultConfig {
                    minSdk = 23
                    consumerProguardFiles("consumer-rules.pro")
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }

                buildTypes {
                    release {
                        // 只有非 -api 结尾的实现模块才启用混淆
                        // API 模块通常不需要混淆，或者只需要简单的混淆规则
                        val isApiModule = project.name.endsWith("-api")
                        if (!isApiModule) {
                            isMinifyEnabled = true
                            proguardFiles(
                                getDefaultProguardFile("proguard-android-optimize.txt"),
                                "proguard-rules.pro"
                            )
                        } else {
                            // API 模块不启用混淆，保持接口清晰
                            isMinifyEnabled = false
                        }
                    }
                    debug {
                        isMinifyEnabled = false
                    }
                }

                buildFeatures {
                    buildConfig = true
                }

                publishing {
                    singleVariant("release") {}
                }
            }
        }

        // 兼容 com.android.application 模块
        project.plugins.withId("com.android.application") {
            project.extensions.configure<BaseAppModuleExtension> {
                namespace = "com.kernelflux.aethersample"
                compileSdk = 36

                defaultConfig {
                    applicationId = "com.kernelflux.aethersample"
                    minSdk = 23
                    targetSdk = 36
                    versionCode = 1
                    versionName = "1.0"

                    multiDexEnabled = true
                }

                buildTypes {
                    debug {
                        isMinifyEnabled = false
                        isDebuggable = true
                    }
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }

                buildFeatures {
                    buildConfig = true
                }

                packaging {
                    resources {
                        // 由于每个模块都有唯一的 moduleName，文件名应该不会冲突
                        // 但为了保险起见，如果确实有重复，使用 pickFirst 策略
                        pickFirsts += "com/kernelflux/fluxrouter/generated/RouteGroupClasses_*.txt"
                        pickFirsts += "com/kernelflux/fluxrouter/generated/ServiceGroupClasses_*.txt"
                    }
                }


            }
        }
    }
}