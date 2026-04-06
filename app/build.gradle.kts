import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Load optional local.properties for API keys — never committed to source control
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

// Load keystore signing credentials from keystore.properties (also git-ignored).
// Falls back gracefully so debug builds work without the file.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace   = "com.bansalcoders.monityx"
    compileSdk  = 36

    defaultConfig {
        applicationId = "com.bansalcoders.monityx"
        minSdk        = 26
        targetSdk     = 36
        versionCode   = 3
        versionName   = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Limit shipped locales to what the app actually supports (cuts ~1 MB from AAB)
        resourceConfigurations += listOf("en")

        // Currency API – key injected from local.properties; free tier works with empty key
        buildConfigField(
            "String", "CURRENCY_API_BASE_URL",
            "\"${localProperties.getProperty("CURRENCY_API_BASE_URL", "https://open.er-api.com/v6/")}\""
        )
        buildConfigField(
            "String", "CURRENCY_API_KEY",
            "\"${localProperties.getProperty("CURRENCY_API_KEY", "")}\""
        )
    }

    // ── Release signing ───────────────────────────────────────────────────────
    // Credentials come from keystore.properties (never committed to git).
    // CI/CD: set KEYSTORE_* environment variables and echo them into the file.
    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProperties.getProperty("storeFile")
            if (storeFilePath != null) {
                storeFile      = rootProject.file(storeFilePath)
                storePassword  = keystoreProperties.getProperty("storePassword", "")
                keyAlias       = keystoreProperties.getProperty("keyAlias", "")
                keyPassword    = keystoreProperties.getProperty("keyPassword", "")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use release signing if keystore.properties is present; otherwise the build
            // will still compile but won't be Play-uploadable without a signature.
            if (keystoreProperties.getProperty("storeFile") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
    }

    // ── Lint ─────────────────────────────────────────────────────────────────
    lint {
        // Fail the release build on errors so regressions are caught early.
        abortOnError   = true
        checkReleaseBuilds = true
        // Ignore known false-positives from generated Compose / Hilt code.
        disable += setOf("ObsoleteLintCustomCheck", "MissingTranslation")
        // Output an HTML report for easy review.
        htmlOutput = file("${project.layout.buildDirectory.asFile.get()}/reports/lint/lint-report.html")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    bundle {
        // Deliver compressed native libraries — saves ~30 % on install size.
        abi { enableSplit = true }
        density { enableSplit = true }
        language { enableSplit = true }
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

// Top-level Kotlin compiler options (AGP 9 forbids kotlin{} inside android{})
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental",    "true")
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Network (currency conversion)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Security
    implementation(libs.security.crypto)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
