plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.bushop.sg"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bushop.sg"
        minSdk = 24
        targetSdk = 34
        versionCode = 16
        versionName = "0.7.1"


        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

/** Locate the debug APK (any filename), verify it has an AndroidManifest, then rename to a predictable name. */
tasks.register("checkAndRenameDebugApk") {
    dependsOn("assembleDebug")
    doLast {
        val apkDir = layout.buildDirectory.dir("outputs/apk/debug").get().asFile
        val apks = apkDir.listFiles { f -> f.name.endsWith(".apk") }
            ?.filterNot { it.name.contains("unsigned", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
        val apk = apks?.firstOrNull()
            ?: throw GradleException("No debug APK found in $apkDir (looked at ${apkDir.absolutePath})")

        // Verify APK has an AndroidManifest.xml (basic integrity check)
        val zipFile = java.util.zip.ZipFile(apk)
        val hasManifest = zipFile.entries().asSequence().any { it.name == "AndroidManifest.xml" }
        val fileCount = zipFile.size()
        val totalBytes = apk.length()
        zipFile.close()

        if (!hasManifest) {
            throw GradleException(
                "APK $apk is CORRUPTED: no AndroidManifest.xml (${totalBytes / 1024} KB, $fileCount files). " +
                "Run ./gradlew clean assembleDebug first."
            )
        }

        val targetName = "app-debug-bus-hop.apk"
        if (apk.name != targetName) {
            val renamed = File(apkDir, targetName)
            apk.renameTo(renamed)
            logger.lifecycle("APK renamed: ${renamed.name} (${renamed.length() / 1024} KB, $fileCount entries ✓)")
        } else {
            logger.lifecycle("APK verified: ${apk.name} (${totalBytes / 1024} KB, $fileCount entries ✓)")
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
}