plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.lims_v3"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lims_v3"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {

    // Dependencies from libs.versions.toml
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // You can remove these as they are already declared above via the version catalog.
    // implementation("androidx.appcompat:appcompat:1.6.1")
    // implementation("com.google.android.material:material:1.9.0")
    // implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // QRコード読み取り用 (JourneyApps ZXing Android Embedded)
    // Correct Kotlin DSL syntax uses parentheses
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.4.1")

    // 通信 (Retrofit - 設定画面のAPI連携用)
    // Correct Kotlin DSL syntax uses parentheses
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}