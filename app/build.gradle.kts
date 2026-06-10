plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import java.io.File
import java.util.zip.ZipFile
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

abstract class CheckAndRenameDebugApk : DefaultTask() {
    @get:InputFiles
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun verifyAndRename() {
        val apkDir = outputDir.get().asFile
        val apk =
            apkDir
                .listFiles { f -> f.name.endsWith(".apk") }
                ?.filterNot { n -> n.name.contains("unsigned", ignoreCase = true) }
                ?.filter { n -> n.length() > 1_000_000 }
                ?.maxByOrNull { n -> n.lastModified() }
                ?: throw GradleException("No valid debug APK found in $apkDir (all candidates < 1 MB — run clean)")

        val totalBytes = apk.length()
        val zf = ZipFile(apk)
        val hasManifest = zf.getEntry("AndroidManifest.xml") != null
        val fileCount = zf.size()
        zf.close()

        if (!hasManifest) {
            throw GradleException(
                "APK $apk CORRUPTED: no AndroidManifest.xml (${totalBytes / 1024} KB, $fileCount files). " +
                    "Run \"./gradlew clean assembleDebug\" first.",
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

android {
    namespace = "com.bushop"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bushop"
        minSdk = 24
        targetSdk = 35
        versionCode = 44
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("RELEASE_KEYSTORE") ?: "${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "androiddebugkey"
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

/** Locate the debug APK, verify it has an AndroidManifest, then rename predictably. */
tasks.register<CheckAndRenameDebugApk>("checkAndRenameDebugApk") {
    outputDir.set(layout.buildDirectory.dir("outputs/apk/debug"))
    dependsOn("assembleDebug")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.compose.ui.test.junit4)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
}

// ── Auto-run APK verification after every assembleDebug ──
afterEvaluate {
    tasks.named("assembleDebug") { finalizedBy("checkAndRenameDebugApk") }
}
