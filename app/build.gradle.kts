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
        versionCode = 23
        versionName = "0.7.8"


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
        isCoreLibraryDesugaringEnabled = true
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

/** Locate the debug APK, verify it has an AndroidManifest, then rename predictably. */
tasks.register("checkAndRenameDebugApk") {
    dependsOn("assembleDebug")
    doLast {
        val apkDir = layout.buildDirectory.dir("outputs/apk/debug").get().asFile
        val apk = apkDir.listFiles { f -> f.name.endsWith(".apk") }
            ?.filterNot { n -> n.name.contains("unsigned", ignoreCase = true) }
            ?.filter { n -> n.length() > 1_000_000 }          // ignore stale artifacts < 1 MB
            ?.maxByOrNull { n -> n.lastModified() }
            ?: throw GradleException("No valid debug APK found in $apkDir (all candidates < 1 MB — run clean)")

        val totalBytes = apk.length()

        // Verify APK has an AndroidManifest.xml using Gradle's zipTree
        val apkTree = project.zipTree(apk)
        val hasManifest = apkTree.matching { include("AndroidManifest.xml") }.files.isNotEmpty()
        val fileCount = apkTree.files.size

        if (!hasManifest) {
            throw GradleException(
                "APK $apk CORRUPTED: no AndroidManifest.xml (${totalBytes / 1024} KB, $fileCount files). " +
                "Run \"./gradlew clean assembleDebug\" first."
            )
        }

        val targetName = "bus-hop.apk"
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

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
}

// ── Auto-run APK verification after every assembleDebug ──
afterEvaluate {
    tasks.named("assembleDebug") { finalizedBy("checkAndRenameDebugApk") }
}