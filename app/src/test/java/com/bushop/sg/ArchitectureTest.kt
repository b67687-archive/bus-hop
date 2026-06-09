package com.bushop.sg

import org.junit.Assert
import org.junit.Test
import java.io.File

/**
 * Architecture constraint tests.
 *
 * Reads source files as text and verifies layer separation rules
 * using simple string matching on import lines.
 *
 * Rule violations fail the test with a descriptive message.
 */
class ArchitectureTest {
    private val projectRoot: File by lazy {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(cwd, cwd.parentFile).filterNotNull()
        val root = candidates.firstOrNull { File(it, "app/build.gradle.kts").exists() }
        requireNotNull(root) {
            "Could not find project root (need app/build.gradle.kts). Tried: " +
                candidates.map { it.absolutePath }
        }
    }

    // ── Domain module must not import android / androidx ──

    @Test
    fun `domain module does not import android or androidx`() {
        val violations = mutableListOf<String>()

        val domainDirs =
            listOf(
                "model",
                "api",
                "repository",
                "usecase",
            )

        val domainRoot = File(projectRoot, "domain/src/main/kotlin/com/bushop/sg/domain")

        for (subDir in domainDirs) {
            File(domainRoot, subDir)
                .walkTopDown()
                .filter { it.extension == "kt" }
                .forEach { file ->
                    val lines = file.readLines()
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("import ")) {
                            if (trimmed.startsWith("import android.") ||
                                trimmed.startsWith("import androidx.")
                            ) {
                                violations.add("${file.name}: $trimmed")
                            }
                        }
                    }
                }
        }

        Assert.assertTrue(
            buildString {
                appendLine("domain/ module must not import android.* or androidx.*:")
                violations.forEach { appendLine("  $it") }
            },
            violations.isEmpty(),
        )
    }

    // ── Release build must have minification enabled ──

    @Test
    fun `release build has isMinifyEnabled = true`() {
        val buildFile = File(projectRoot, "app/build.gradle.kts")
        val content = buildFile.readText()

        val releaseBlock =
            Regex(
                """release\s*\{(.*?)\}""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE),
            ).find(content)

        Assert.assertNotNull("Could not find 'release' block in app/build.gradle.kts", releaseBlock)
        Assert.assertTrue(
            "release block must contain 'isMinifyEnabled = true', found:\n${releaseBlock!!.value}",
            releaseBlock.value.contains("isMinifyEnabled = true"),
        )
    }

    // ── No hardcoded dependency strings ──

    @Test
    fun `no hardcoded dependency strings in build-gradle-kts`() {
        val buildFile = File(projectRoot, "app/build.gradle.kts")
        val content = buildFile.readLines()

        val hardcodedPattern =
            Regex(
                """implementation\(["'](androidx|com\.squareup|org\.jetbrains|junit)""",
                RegexOption.IGNORE_CASE,
            )

        val violations =
            content.mapIndexedNotNull { idx, line ->
                if (hardcodedPattern.containsMatchIn(line.trim())) {
                    "line ${idx + 1}: ${line.trim()}"
                } else {
                    null
                }
            }

        Assert.assertTrue(
            buildString {
                appendLine("Found hardcoded dependency strings — use libs.* instead:")
                violations.forEach { appendLine("  $it") }
            },
            violations.isEmpty(),
        )
    }

    // ── ProGuard keep rules must reference existing packages ──

    @Test
    fun `proguard keep rules match existing packages`() {
        val proguardFile = File(projectRoot, "app/proguard-rules.pro")
        Assert.assertTrue("proguard-rules.pro not found", proguardFile.exists())
        val lines = proguardFile.readLines()
        val violations = mutableListOf<String>()
        val keepPattern = Regex("""^-keep\s+(class\s+[\w.]+)""")
        val keepAllowObfuscationPattern = Regex("""^-keep,allowobfuscation\s+(class\s+[\w.]+)""")
        for (line in lines) {
            val trimmed = line.trim()
            for (pattern in listOf(keepPattern, keepAllowObfuscationPattern)) {
                val match = pattern.find(trimmed)
                if (match != null) {
                    val classSpec = match.groupValues[1]
                    val packagePart =
                        classSpec
                            .removePrefix("class ")
                            .removeSuffix(".**")
                            .removeSuffix(".*")
                            .replace('.', '/')
                    val domainDir = File(projectRoot, "domain/src/main/kotlin/$packagePart")
                    val dataDir = File(projectRoot, "data/src/main/kotlin/$packagePart")
                    val appDir = File(projectRoot, "app/src/main/kotlin/$packagePart")
                    if (!domainDir.exists() && !dataDir.exists() && !appDir.exists()) {
                        violations.add("'$trimmed' — package '$packagePart' not found in any module")
                    }
                }
            }
        }
        Assert.assertTrue(
            buildString {
                appendLine("ProGuard -keep rules reference non-existent packages:")
                violations.forEach { appendLine("  $it") }
            },
            violations.isEmpty(),
        )
    }
}
