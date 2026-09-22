plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.miyu.companion"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.miyu.companion"
        minSdk = 26
        targetSdk = 34
        versionCode = 15
        versionName = "1.2.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        resourceConfigurations += listOf("ar", "en")

        // Owner Edition wiring. Public builds get MIYU_OWNER_BUILD=false and empty values, so the
        // Owner Panel does not exist in a public APK. The private workflow exports the salt/hash
        // produced from the MIYU_OWNER_PIN secret at build time - the PIN itself never exists here.
        val ownerSalt = System.getenv("MIYU_OWNER_SALT").orEmpty()
        val ownerHash = System.getenv("MIYU_OWNER_HASH").orEmpty()
        val ownerBuild = ownerSalt.isNotBlank() && ownerHash.isNotBlank()
        buildConfigField("boolean", "MIYU_OWNER_BUILD", ownerBuild.toString())
        buildConfigField("String", "MIYU_OWNER_SALT", "\"$ownerSalt\"")
        buildConfigField("String", "MIYU_OWNER_HASH", "\"$ownerHash\"")
        buildConfigField("String", "MIYU_BUILD_ID", "\"${System.getenv("MIYU_BUILD_ID") ?: "public-android-1.2.5"}\"")
    }

    // CI signing: when the public workflow generates an ephemeral keystore it exports
    // MIYU_CI_KEYSTORE so release APKs are installable without shipping a real key.
    // No keystore, no password and no PIN is ever committed to this repository.
    signingConfigs {
        val ciStore = System.getenv("MIYU_CI_KEYSTORE")
        if (ciStore != null && ciStore.isNotBlank()) {
            create("ci") {
                storeFile = file(ciStore)
                storePassword = System.getenv("MIYU_CI_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MIYU_CI_KEY_ALIAS") ?: "miyu"
                keyPassword = System.getenv("MIYU_CI_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val ci = signingConfigs.findByName("ci")
            if (ci != null) {
                signingConfig = ci
            }
        }
        debug {
            isDebuggable = true
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        // Warnings (e.g. deprecated Divider) must never block a release build.
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.savedstate:savedstate:1.2.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WebView for the shared portable core (optional in-app fallback view)
    implementation("androidx.webkit:webkit:1.11.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
