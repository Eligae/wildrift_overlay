import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(FileInputStream(file))
}

val apiBaseUrl: String = localProps.getProperty("WR_API_BASE_URL") ?: "http://192.168.0.35:3000/"
val apiToken: String = localProps.getProperty("WR_API_TOKEN") ?: ""
val releaseKeystorePath: String = localProps.getProperty("RELEASE_KEYSTORE_PATH") ?: ""
val releaseKeystorePassword: String = localProps.getProperty("RELEASE_KEYSTORE_PASSWORD") ?: ""
val releaseKeyAlias: String = localProps.getProperty("RELEASE_KEY_ALIAS") ?: ""
val releaseKeyPassword: String = localProps.getProperty("RELEASE_KEY_PASSWORD") ?: ""

android {
    namespace = "com.eligae.wildrift.overlay"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eligae.wildrift.overlay"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "API_TOKEN", "\"$apiToken\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (releaseKeystorePath.isNotEmpty()) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystorePath.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.coil)
    implementation(libs.mlkit.text.recognition.korean)
}
