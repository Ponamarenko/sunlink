package com.sunlink.svitgrid.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.sunlink.svitgrid.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val changelog: String
)

object UpdateManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/Ponamarenko/sunlink/releases/latest")
                .header("User-Agent", "SunLink-App/${BuildConfig.VERSION_NAME}")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val tag = json.optString("tag_name", "")
                val changelog = json.optString("body", "")
                val assets = json.optJSONArray("assets")
                var downloadUrl = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.optJSONObject(i) ?: continue
                        val name = a.optString("name", "")
                        val url = a.optString("browser_download_url", "")
                        if (name.endsWith(".apk") && url.isNotBlank()) {
                            downloadUrl = url
                            if (name.contains("debug")) break
                        }
                    }
                    if (downloadUrl.isBlank() && assets.length() > 0) {
                        downloadUrl = assets.optJSONObject(0)?.optString("browser_download_url", "") ?: ""
                    }
                }
                if (tag.isBlank() || downloadUrl.isBlank()) {
                    return@withContext Result.failure(Exception("Некоректна відповідь GitHub"))
                }
                val latest = normalizeVersion(tag)
                val current = normalizeVersion(BuildConfig.VERSION_NAME)
                if (isNewer(latest, current)) {
                    Result.success(UpdateInfo(tag, downloadUrl, changelog))
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeVersion(v: String): List<Int> {
        return v.trim().removePrefix("v").removePrefix("V")
            .split('.', '-', '_')
            .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }
    }

    private fun isNewer(latest: List<Int>, current: List<Int>): Boolean {
        val max = maxOf(latest.size, current.size)
        for (i in 0 until max) {
            val l = latest.getOrElse(i) { 0 }
            val c = current.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    suspend fun downloadAndInstall(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: return@withContext Result.failure(Exception("Немає каталогу завантажень"))
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, "sunlink-update.apk")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SunLink-App/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body ?: return@withContext Result.failure(Exception("Порожня відповідь"))
                val total = body.contentLength()
                body.byteStream().use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var read: Int
                        var downloaded = 0L
                        var lastPct = -1
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                val pct = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                                if (pct != lastPct) {
                                    lastPct = pct
                                    withContext(Dispatchers.Main) { onProgress(pct) }
                                }
                            }
                        }
                        output.flush()
                    }
                }
            }
            withContext(Dispatchers.Main) {
                onProgress(100)
                installApk(context, outFile)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun installApk(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
