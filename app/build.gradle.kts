plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.premium.myreader"
    compileSdk = 34

    signingConfigs {
        create("release") {
            storeFile = file("myreader-release-key.jks")
            storePassword = "myreader123"
            keyAlias = "myreader"
            keyPassword = "myreader123"
        }
    }

    defaultConfig {
        applicationId = "com.premium.myreader"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"
        multiDexEnabled = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.4.3" }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2") 
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // Firebase Core & Analytics (New)
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Biometric Authentication (New)
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Coil & Accompanist
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")
}

dependencies { implementation("com.google.android.gms:play-services-auth:20.7.0") }

dependencies { implementation("io.coil-kt:coil-compose:2.5.0") }

dependencies { implementation("com.google.ai.client.generativeai:generativeai:0.9.0") }
dependencies { implementation("io.coil-kt:coil-compose:2.6.0") }
