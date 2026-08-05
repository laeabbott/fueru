import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Secrets live in local.properties (machine-local, gitignored), never in a checked-in source file.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.fueru.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fueru.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // USDA FoodData Central — see data/food/UsdaFoodApi.kt. Empty string (not a crash) if unset.
        buildConfigField("String", "USDA_API_KEY", "\"${localProperties.getProperty("USDA_API_KEY", "")}\"")
        // Giphy — see data/celebration/GiphyApi.kt. Empty string (not a crash) if unset.
        buildConfigField("String", "GIPHY_API_KEY", "\"${localProperties.getProperty("GIPHY_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Hard-pin kotlin-stdlib (and friends) to our Kotlin plugin's version on every configuration,
// including the KSP-generated sources' own compile classpath. A BOM only sets a *preferred*
// version, which a transitive "requires" elsewhere can still outrank; force() cannot be outranked.
configurations.all {
    resolutionStrategy {
        force(
            "org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}",
            "org.jetbrains.kotlin:kotlin-stdlib-common:${libs.versions.kotlin.get()}",
            "org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin.get()}",
        )
    }
}

dependencies {
    // Pins every transitive kotlin-stdlib dependency to the same version as our Kotlin plugin,
    // so a newer stdlib pulled in by some other library can't outrun the compiler.
    implementation(platform(libs.kotlin.bom))
    androidTestImplementation(platform(libs.kotlin.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    // Coil3 splits network image fetching out of coil-compose — without this, any https:// model
    // (the on-demand exercise catalog's images, Giphy gifs) silently fails to load; only
    // file:///android_asset/ local images worked without it, which is exactly the bug reported.
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
