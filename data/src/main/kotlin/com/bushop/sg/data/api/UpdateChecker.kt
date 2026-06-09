package com.bushop.sg.data.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val hasUpdate: Boolean,
)

/** Checks GitHub releases and downloads APK updates. */
class UpdateChecker {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    private val gson = Gson()
    private val apiUrl = "https://api.github.com/repos/B67687/BusHop-SG/releases/latest"

    /** Fetch latest release info from GitHub. */
    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val request =
                    Request
                        .Builder()
                        .url(apiUrl)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext null
                val release = gson.fromJson(body, GitHubRelease::class.java)
                val tag = release.tagName.removePrefix("v")
                val apkAsset = release.assets?.find { it.name.endsWith(".apk") }
                if (apkAsset == null || !isNewerVersion(tag, currentVersion)) return@withContext null
                UpdateInfo(
                    latestVersion = tag,
                    downloadUrl = apkAsset.browserDownloadUrl,
                    releaseNotes = release.body?.take(500) ?: "",
                    hasUpdate = true,
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }

    /** Download APK to local file. */
    suspend fun downloadApk(
        url: String,
        targetFile: File,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body ?: return@withContext false
                FileOutputStream(targetFile).use { output -> body.byteStream().use { it.copyTo(output) } }
                true
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
        }

    private fun isNewerVersion(
        tag: String,
        current: String,
    ): Boolean {
        val tParts = tag.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(tParts.size, cParts.size)) {
            val t = tParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (t != c) return t > c
        }
        return false
    }

    internal data class GitHubRelease(
        @SerializedName("tag_name") val tagName: String,
        @SerializedName("body") val body: String?,
        @SerializedName("assets") val assets: List<GitHubAsset>?,
    )

    internal data class GitHubAsset(
        @SerializedName("name") val name: String,
        @SerializedName("browser_download_url") val browserDownloadUrl: String,
    )
}
