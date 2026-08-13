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

        versionCode = 2

        versionName = "1.0.1"

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

    implementation("com.google.android.gms:play-services-ads:23.6.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("androidx.room:room-runtime:2.6.1")

    implementation("androidx.room:room-ktx:2.6.1")

    ksp("androidx.room:room-compiler:2.6.1")



    debugImplementation("androidx.compose.ui:ui-tooling")

}

