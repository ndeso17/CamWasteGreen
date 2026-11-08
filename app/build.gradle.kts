plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

/**
 * Properti rahasia dibaca dari `local.properties` (di luar versi kontrol).
 * Bila berkas atau kuncinya tidak ada, nilainya kosong dan build tetap jalan;
 * server kemudian menolak melayani karena token kosong (WP-25).
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun secretProperty(name: String): String =
    (localProperties.getProperty(name) ?: "").replace("\\", "\\\\").replace("\"", "\\\"")

/**
 * Kunci rilis dibaca dari luar repo (`~/.config/camwastegreen/`). Bila berkas
 * tidak ada, build rilis tetap jalan tetapi tidak ditandatangani; jangan
 * mengunggah APK tanpa tanda tangan.
 */
val signingDir: File = File(System.getProperty("user.home"), ".config/camwastegreen")
val signingProperties: Properties = Properties().apply {
    val file = File(signingDir, "signing.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val releaseKeystoreFile: File = File(signingDir, "release.jks")

val allowedAddresses: String = secretProperty("camwastegreen.allowedAddresses")

fun generateApkFileName(buildType: String): String {
    val now = Date()
    val day = SimpleDateFormat("dd", Locale("id")).format(now)
    val month = SimpleDateFormat("MMMM", Locale("id")).format(now) // Juli
    val year = SimpleDateFormat("yyyy", Locale("id")).format(now)
    val time = SimpleDateFormat("HHmm", Locale("id")).format(now)
    return "IPCam_${day}${month}${year}_${time}.apk"
}

android {
    namespace = "dev.ndeso17.camwastegreen"
    compileSdk = 36  // Diupdate dari 34 ke 36

    signingConfigs {
        create("release") {
            if (releaseKeystoreFile.exists() && signingProperties.isNotEmpty()) {
                storeFile = releaseKeystoreFile
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "dev.ndeso17.camwastegreen"
        minSdk = 24
        targetSdk = 34  // Bisa tetap 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Daftar alamat yang diizinkan (dipisah koma; kosong berarti tanpa batas
        // alamat). Token tidak lagi berasal dari build: dibuat saat server start.
        buildConfigField("String", "ALLOWED_ADDRESSES", "\"$allowedAddresses\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseKeystoreFile.exists() && signingProperties.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    applicationVariants.configureEach {
        val variant = this
        variant.outputs.configureEach {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = generateApkFileName(variant.buildType.name)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    // NanoHTTPD web server
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("org.nanohttpd:nanohttpd-webserver:2.3.1")
    implementation(libs.androidx.graphics.core)

    // Paket inferensi wajah/segmentasi dan ekstensi kamera sengaja tidak
    // dipakai: aplikasi ini hanya menangkap dan menyajikan frame (CWG-N02).

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}