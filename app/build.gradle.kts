plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    // Independent wire-format encoder used only in JVM tests; absent from the APK.
    testImplementation("com.google.protobuf:protobuf-java:3.25.5")
}

android {
    namespace = "de.bcycle.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.bcycle.app"
        minSdk = 33
        targetSdk = 35
        versionCode = 5
        versionName = "0.1.4"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
