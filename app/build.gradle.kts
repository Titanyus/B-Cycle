plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Resolve Android test SDKs through Gradle's configured repositories/proxy.
val robolectricSdk33 by configurations.creating
val robolectricSdk35 by configurations.creating

dependencies {
    implementation("org.mapsforge:mapsforge-map-android:0.25.0")
    testImplementation("junit:junit:4.13.2")
    // Independent wire-format encoder used only in JVM tests; absent from the APK.
    testImplementation("com.google.protobuf:protobuf-java:3.25.5")
    // Native Android layout/font verification only; absent from the APK.
    testImplementation("org.robolectric:robolectric:4.14.1")
    // Official Garmin decoder/profile validation only, absent from the APK.
    testImplementation("com.garmin:fit:21.218.0")
    robolectricSdk33("org.robolectric:android-all-instrumented:13-robolectric-9030017-i7")
    robolectricSdk35("org.robolectric:android-all-instrumented:15-robolectric-12650502-i7")
}

android {
    namespace = "de.bcycle.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.bcycle.app"
        minSdk = 33
        targetSdk = 35
        versionCode = 21
        versionName = "0.6.5"
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
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            val sdkDir = layout.buildDirectory.dir("robolectric-sdk").get().asFile
            it.systemProperty("robolectric.offline", "true")
            it.systemProperty("robolectric.dependency.dir", sdkDir.absolutePath)
            it.doFirst { copy { from(robolectricSdk33, robolectricSdk35); into(sdkDir) } }
        }
    }
}
