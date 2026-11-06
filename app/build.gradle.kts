import java.util.Properties

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val myketRsaKey = localProps.getProperty("MYKET_RSA_KEY") ?: ""

plugins {
    id("com.android.application") version  "8.5.2"
    id("org.jetbrains.kotlin.android") version "1.9.24"

}

android {
    signingConfigs {
    create("release") {
        storeFile = file("my-release-key.jks")
        storePassword = "123456"
        keyAlias = "my-key-alias"
        keyPassword = "123456"
    }
}
buildTypes {
    getByName("release") {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = false
    }
}
lint {
    checkReleaseBuilds = false
    abortOnError = false
}
    namespace = "ir.cryptoyar.app4"
    compileSdk = 34

    defaultConfig {
        applicationId = "ir.cryptoyar.app4"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        manifestPlaceholders["marketApplicationId"] = "ir.mservices.market"
        manifestPlaceholders["marketBindAddress"] = "ir.mservices.market.InAppBillingService.BIND"
        manifestPlaceholders["marketPermission"] = "ir.mservices.market.BILLING"
        versionName = "1.0"
        buildConfigField("String", "MYKET_RSA_KEY", "\"$myketRsaKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.6.4")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
   
   
}
