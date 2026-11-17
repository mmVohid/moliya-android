plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "uz.moliya.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "uz.moliya.app"
        minSdk = 26
        targetSdk = 34
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.0"
        buildConfigField("String", "GIT_SHA", "\"${System.getenv("GITHUB_SHA") ?: "dev"}\"")
    }

    // Barqaror imzo: har yangi versiya eskisi ustiga o'rnatiladi (o'chirib qayta o'rnatish shart emas)
    signingConfigs {
        getByName("debug") {
            storeFile = file("moliya-debug.jks")
            storePassword = "moliya2026"
            keyAlias = "moliya"
            keyPassword = "moliya2026"
        }
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

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
