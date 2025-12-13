import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.flux.router)
    id("com.kernelflux.mobile.androidconfig")
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties()
if (keystorePropsFile.exists()) {
    keystoreProps.load(keystorePropsFile.inputStream())
}
// helper to read env if property missing
fun propOrEnv(key: String): String? =
    (keystoreProps.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() }

android {


    signingConfigs {
        create("release") {
            storeFile = file(propOrEnv("storeFile") ?: "")
            storePassword = propOrEnv("storePassword")
            keyAlias = propOrEnv("keyAlias")
            keyPassword = propOrEnv("keyPassword")
        }
    }



    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

}

dependencies {
    implementation(libs.flux.router.core)
    implementation(libs.androidx.recyclerview)
    ksp(libs.flux.router.compiler.ksp)
    ksp(libs.glide.ksp)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)

    // 功能模块 API
    debugImplementation(project(":aether-imageloader-api"))
    releaseImplementation(libs.aether.imageloader.api)


    implementation(project(":aether-network-api"))
    implementation(project(":aether-log-api"))

    // 功能模块实现
    debugImplementation(project(":aether-imageloader-glide"))
    releaseImplementation(libs.aether.imageloader.glide)


    implementation(project(":aether-network-okhttp"))
    implementation(project(":aether-log-android"))

    // 业务模块 API
    implementation(project(":aether-payment-api"))
    implementation(project(":aether-share-api"))
    implementation(project(":aether-login-api"))

    // 业务模块实现
    implementation(project(":aether-payment-alipay"))
    implementation(project(":aether-share-wechat"))
    implementation(project(":aether-login-oauth"))


}

ksp {
    arg("moduleName", "sample")
}