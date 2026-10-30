import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The app file is chosen with -PappJson=path/to/app.json and must already be checked by tools/validate.py.
val appJson = rootProject.file(providers.gradleProperty("appJson").orElse("../../apps/tally.json").get())
@Suppress("UNCHECKED_CAST")
val spec = JsonSlurper().parse(appJson) as Map<String, Any>
val appId = spec["id"] as String
require(Regex("^[a-z][a-z0-9]{1,30}$").matches(appId)) { "invalid app id; run tools/validate.py first" }

// Bundle the app file as an asset; the runtime draws whatever it describes.
val generatedAssets = layout.buildDirectory.dir("generated/wristblocks/assets").get().asFile
generatedAssets.mkdirs()
appJson.copyTo(File(generatedAssets, "app.json"), overwrite = true)

android {
    namespace = "org.tuplelabs.wristblocks"
    compileSdk = 37
    defaultConfig {
        applicationId = "org.tuplelabs.wristblocks.$appId"
        minSdk = 30
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        resValue("string", "app_name", spec["name"] as String)
    }
    buildFeatures { compose = true; resValues = true }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // shortcut: signed with the machine's debug key so anyone can install it, swap for a real key before store release
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    onVariants { it.sources.assets?.addStaticSourceDirectory(generatedAssets.absolutePath) }
}

dependencies {
    implementation("androidx.wear.compose:compose-material3:1.7.1")
    implementation("androidx.wear.compose:compose-foundation:1.7.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
}
