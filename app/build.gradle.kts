import java.time.Instant

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Resolve Android test SDKs through Gradle's configured repositories/proxy.
val robolectricSdk33 by configurations.creating
val robolectricSdk35 by configurations.creating
val robolectricSdk36 by configurations.creating

dependencies {
    implementation("org.mapsforge:mapsforge-map-android:0.25.0")
    // Official streaming FIT decoder, reused for activity metadata and map previews.
    implementation("com.garmin:fit:21.218.0")
    // Android URLConnection rejects PROPFIND/MKCOL; WebDAV needs a full HTTP client.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.squareup.okhttp3:okhttp-tls:4.12.0")
    testImplementation("junit:junit:4.13.2")
    // Independent wire-format encoder used only in JVM tests; absent from the APK.
    testImplementation("com.google.protobuf:protobuf-java:3.25.5")
    // Native Android layout/font verification only; absent from the APK.
    testImplementation("org.robolectric:robolectric:4.16.1")
    robolectricSdk33("org.robolectric:android-all-instrumented:13-robolectric-9030017-i7")
    robolectricSdk35("org.robolectric:android-all-instrumented:15-robolectric-13954326-i7")
    robolectricSdk36("org.robolectric:android-all-instrumented:16-robolectric-13921718-i7")
}

android {
    namespace = "de.bcycle.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "de.bcycle.app"
        minSdk = 33
        targetSdk = 36
        versionCode = 74
        versionName = "0.8.4"
        // Captured at build configuration, never from the app launch clock.
        val buildEpoch = providers.environmentVariable("SOURCE_DATE_EPOCH").orNull?.toLong()
            ?: Instant.now().epochSecond
        buildConfigField("long", "BUILD_TIMESTAMP", "${buildEpoch}L")
    }
    buildFeatures { buildConfig = true }
    buildTypes {
        release {
            // Keep updates compatible with the existing test installation; CI may supply its stable key.
            signingConfig = signingConfigs.getByName("debug")
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
            it.doFirst { copy { from(robolectricSdk33, robolectricSdk35, robolectricSdk36); into(sdkDir) } }
        }
    }
}
