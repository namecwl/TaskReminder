package com.example.taskreminder.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val apkUrl: String,
    val releaseUrl: String,
    val notes: String
)

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

object UpdateManager {
    private const val RepositoryApi = "https://api.github.com/repos/namecwl/TaskReminder/releases/latest"

    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = openConnection(RepositoryApi)
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    error("GitHub 返回 HTTP $responseCode")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val release = JSONObject(body)
                val remoteVersion = release.optString("tag_name")
                    .ifBlank { release.optString("name") }
                    .trim()
                    .removePrefix("v")
                if (!isNewerVersion(remoteVersion, currentVersion)) {
                    return@runCatching UpdateCheckResult.UpToDate
                }

                val assets = release.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (index in 0 until assets.length()) {
                        val asset = assets.optJSONObject(index) ?: continue
                        val name = asset.optString("name")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            if (apkUrl.isNotBlank()) break
                        }
                    }
                }
                if (apkUrl.isNullOrBlank()) {
                    return@runCatching UpdateCheckResult.Failed("最新 Release 还没有上传 APK 文件")
                }

                UpdateCheckResult.Available(
                    UpdateInfo(
                        version = remoteVersion,
                        apkUrl = apkUrl,
                        releaseUrl = release.optString("html_url"),
                        notes = release.optString("body")
                    )
                )
            }.getOrElse { throwable ->
                UpdateCheckResult.Failed(throwable.message ?: "检查更新失败")
            }
        }

    suspend fun downloadApk(context: Context, info: UpdateInfo): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val safeVersion = info.version.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val target = File(updateDir, "TaskReminder-$safeVersion.apk")
                val temporary = File(updateDir, "TaskReminder-$safeVersion.apk.part")

                val connection = openConnection(info.apkUrl)
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    error("下载失败，HTTP $responseCode")
                }
                connection.inputStream.use { input ->
                    temporary.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (temporary.length() < 1024L) {
                    temporary.delete()
                    error("下载的 APK 无效")
                }
                if (target.exists()) target.delete()
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }
                target
            }
        }

    /** 返回 true 表示已打开安装器；false 表示已打开“允许安装未知应用”设置页。 */
    fun installApk(context: Context, apkFile: File): Result<Boolean> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return@runCatching false
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "TaskReminder-Android-Updater")
        }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = versionParts(remote)
        val currentParts = versionParts(current)
        if (remoteParts.isEmpty() || currentParts.isEmpty()) return false

        val size = maxOf(remoteParts.size, currentParts.size)
        for (index in 0 until size) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (remotePart != currentPart) return remotePart > currentPart
        }
        return false
    }

    private fun versionParts(version: String): List<Int> =
        version
            .trim()
            .removePrefix("v")
            .split(Regex("[^0-9]+"))
            .mapNotNull(String::toIntOrNull)
}
