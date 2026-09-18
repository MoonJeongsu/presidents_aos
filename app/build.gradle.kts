plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.uspresident.speeches"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.uspresident.speeches"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.0.4"

        buildConfigField("String", "CAULY_APP_CODE", "\"joVR3oib\"")
        buildConfigField("String", "PANGLE_APP_ID", "\"8896838\"")
        buildConfigField("String", "PANGLE_INTERSTITIAL_SLOT_ID", "\"983641217\"")
        // Create a Banner placement in Pangle console and put the slot ID here.
        buildConfigField("String", "PANGLE_BANNER_SLOT_ID", "\"\"")
        buildConfigField("String", "UNITY_GAME_ID", "\"800374070\"")
        buildConfigField("String", "UNITY_INTERSTITIAL_PLACEMENT_ID", "\"BP_Interstitial_Android\"")
        buildConfigField("String", "UNITY_BANNER_PLACEMENT_ID", "\"BP_Banner_Android\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    sourceSets.getByName("main") {
        assets.exclude("speeches/**")
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.google.android.gms:play-services-ads-identifier:18.1.0")
    implementation("com.google.android.gms:play-services-appset:16.1.0")
    implementation("com.fsn.cauly:cauly-sdk:3.5.46")
    implementation("com.pangle.global:pag-sdk:7.9.0.9")
    implementation("com.unity3d.ads:unity-ads:4.16.6")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
