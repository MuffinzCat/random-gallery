import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

fun signingValues(prefix: String) =
    mapOf(
        "storeFile" to System.getenv("${prefix}_STORE_FILE"),
        "storePassword" to System.getenv("${prefix}_STORE_PASSWORD"),
        "keyAlias" to System.getenv("${prefix}_KEY_ALIAS"),
        "keyPassword" to System.getenv("${prefix}_KEY_PASSWORD"),
    )

val communitySigningValues = signingValues("RG_COMMUNITY_SIGNING")
val playSigningValues = signingValues("RG_PLAY_SIGNING")

val requestedTasks = gradle.startParameter.taskNames.map { it.lowercase() }
val allReleaseSigningRequested = requestedTasks.any { it.endsWith("assemblerelease") || it.endsWith("bundlerelease") }
val communitySigningRequested = allReleaseSigningRequested || requestedTasks.any { "communityrelease" in it }
val playSigningRequested = allReleaseSigningRequested || requestedTasks.any { "playrelease" in it }

fun requireSigningCredentials(name: String, requested: Boolean, values: Map<String, String?>) {
    if (requested) {
        val missing = values.filterValues { it.isNullOrBlank() }.keys
        require(missing.isEmpty()) {
            "$name signing credentials are missing: ${missing.joinToString()}"
        }
    }
}

requireSigningCredentials("Community release", communitySigningRequested, communitySigningValues)
requireSigningCredentials("Play release", playSigningRequested, playSigningValues)

android {
    namespace = "com.lvlh2.randomgallery"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.muffinzcat.randomgallery"
        minSdk = 24
        targetSdk = 37
        versionCode = 23
        versionName = "2.1.0-muffin.3"
    }


    signingConfigs {
        create("communityRelease") {
            if (communitySigningValues.values.all { !it.isNullOrBlank() }) {
                storeFile = file(requireNotNull(communitySigningValues["storeFile"]))
                storePassword = communitySigningValues["storePassword"]
                keyAlias = communitySigningValues["keyAlias"]
                keyPassword = communitySigningValues["keyPassword"]
            }
        }
        create("playRelease") {
            if (playSigningValues.values.all { !it.isNullOrBlank() }) {
                storeFile = file(requireNotNull(playSigningValues["storeFile"]))
                storePassword = playSigningValues["storePassword"]
                keyAlias = playSigningValues["keyAlias"]
                keyPassword = playSigningValues["keyPassword"]
            }
        }
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("community") {
            dimension = "distribution"
            applicationId = "com.muffinzcat.randomgallery.community"
        }
        create("play") {
            dimension = "distribution"
            applicationId = "com.muffinzcat.randomgallery"
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
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

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Single output name for every variant (old Expo build named its APK random-gallery.apk).
    // The public VariantOutput interface doesn't expose outputFileName, but the runtime
    // objects are VariantOutputImpl, which does — set it there (AGP-8 way, no deprecated API).
    androidComponents {
        onVariants { variant ->
            if (variant.buildType == "release") {
                val distribution = variant.productFlavors.single { it.first == "distribution" }.second
                val signingConfigName = when (distribution) {
                    "community" -> "communityRelease"
                    "play" -> "playRelease"
                    else -> error("Unknown distribution flavour: $distribution")
                }
                variant.signingConfig.setConfig(signingConfigs.getByName(signingConfigName))
            }

            variant.outputs.forEach { output ->
                (output as com.android.build.api.variant.impl.VariantOutputImpl)
                    .outputFileName = "random-gallery-muffin.apk"
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ktlint {
    // Compose convention: composable functions use PascalCase (e.g. RandomGalleryTheme)
    additionalEditorconfig.set(
        mapOf(
            "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
        ),
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.coil.compose)
    implementation(libs.telephoto.zoomable.coil3)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
