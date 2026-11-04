import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

val releaseSigningValues =
    mapOf(
        "storeFile" to System.getenv("RG_SIGNING_STORE_FILE"),
        "storePassword" to System.getenv("RG_SIGNING_STORE_PASSWORD"),
        "keyAlias" to System.getenv("RG_SIGNING_KEY_ALIAS"),
        "keyPassword" to System.getenv("RG_SIGNING_KEY_PASSWORD"),
    )

val releaseSigningRequested =
    gradle.startParameter.taskNames.any {
        it.endsWith("assembleRelease", ignoreCase = true) ||
            it.endsWith("bundleRelease", ignoreCase = true)
    }

if (releaseSigningRequested) {
    val missing = releaseSigningValues.filterValues { it.isNullOrBlank() }.keys
    require(missing.isEmpty()) {
        "Release signing credentials are missing: ${missing.joinToString()}"
    }
}
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
        create("release") {
            if (releaseSigningValues.values.all { !it.isNullOrBlank() }) {
                storeFile = file(requireNotNull(releaseSigningValues["storeFile"]))
                storePassword = releaseSigningValues["storePassword"]
                keyAlias = releaseSigningValues["keyAlias"]
                keyPassword = releaseSigningValues["keyPassword"]
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
