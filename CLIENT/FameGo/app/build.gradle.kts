import java.util.Properties

val fameGoLocalProperties = Properties()
val fameGoLocalPropertiesFile = rootProject.file("local.properties")
if (fameGoLocalPropertiesFile.exists()) {
  fameGoLocalPropertiesFile.inputStream().use { fameGoLocalProperties.load(it) }
}
fun fameGoConfig(name: String): String =
  fameGoLocalProperties.getProperty(name) ?: System.getenv(name) ?: ""
fun fameGoGradleString(value: String): String =
  value.replace("\\", "\\\\").replace("\"", "\\\"")

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.famebros.famego"
    minSdk = 24
    targetSdk = 36
    versionCode = 2
    versionName = "OSEM A.1.1"

    buildConfigField("String", "SUPABASE_URL", "\"${fameGoGradleString(fameGoConfig("SUPABASE_URL"))}\"")
    buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${fameGoGradleString(fameGoConfig("SUPABASE_PUBLISHABLE_KEY"))}\"")
    buildConfigField("String", "MAPTILER_KEY", "\"${fameGoGradleString(fameGoConfig("MAPTILER_KEY"))}\"")
    // FCM sender config (empty until the Firebase project exists — push stays inert).
    buildConfigField("String", "FCM_SENDER_ID", "\"${fameGoGradleString(fameGoConfig("FCM_SENDER_ID"))}\"")
    buildConfigField("String", "FCM_API_KEY", "\"${fameGoGradleString(fameGoConfig("FCM_API_KEY"))}\"")
    buildConfigField("String", "FCM_PROJECT_ID", "\"${fameGoGradleString(fameGoConfig("FCM_PROJECT_ID"))}\"")
    buildConfigField("String", "FCM_APP_ID", "\"${fameGoGradleString(fameGoConfig("FCM_APP_ID"))}\"")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      // Only wire the file when it actually exists so plain `assembleRelease`
      // doesn't fail on machines without the upload key.
      if (file(keystorePath).exists()) {
        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Use the upload key only when fully configured; otherwise fall back to
      // the debug key so release builds still work locally.
      val releaseKey = signingConfigs.getByName("release")
      signingConfig = if (releaseKey.storeFile?.exists() == true) releaseKey else signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Every built APK carries the FameGo name (e.g. FameGo-debug.apk).
base {
  archivesName.set("FameGo")
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // FCM for background push (inert until FCM_* values are set in local.properties).
  implementation("com.google.firebase:firebase-messaging:24.1.0")
  // Free interactive maps: osmdroid (OSM) + CARTO dark tiles, no API key.
  implementation("org.osmdroid:osmdroid-android:6.1.20")
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
