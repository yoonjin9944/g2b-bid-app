package com.g2b.bidapp.data.version

import com.g2b.bidapp.BuildConfig
import com.g2b.bidapp.domain.model.VersionCheckResult
import com.g2b.bidapp.domain.model.VersionInfo
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class VersionCheckRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {

    protected open val versionJsonUrl: String =
        "https://yoonjin9944.github.io/g2b-bid-app/version.json"

    protected open val currentVersion: String
        get() = BuildConfig.VERSION_NAME // 호출할 때마다 값을 확인

    protected open val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    suspend fun checkVersion(): VersionCheckResult = withContext(ioDispatcher) {
        try {
            val request = Request.Builder()
                .url(versionJsonUrl)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext VersionCheckResult.Error("HTTP ${response.code}")
                }
                val body = response.body?.string()
                    ?: return@withContext VersionCheckResult.Error("Empty response")

                val info = parseVersionInfo(body)
                compare(info)
            }
        } catch (e: Exception) {
            VersionCheckResult.Error(e.message ?: "Unknown error")
        }
    }

    private val gson = Gson()

    private fun parseVersionInfo(json: String): VersionInfo {
        val dto = gson.fromJson(json, VersionInfoDto::class.java)
        return VersionInfo(
            latestVersion = dto.latestVersion ?: error("latestVersion missing"),
            minRequiredVersion = dto.minRequiredVersion ?: error("minRequiredVersion missing"),
            downloadUrl = dto.downloadUrl ?: error("downloadUrl missing"),
            releaseNotes = dto.releaseNotes.orEmpty(),
        )
    }

    private data class VersionInfoDto(
        @SerializedName("latestVersion") val latestVersion: String?,
        @SerializedName("minRequiredVersion") val minRequiredVersion: String?,
        @SerializedName("downloadUrl") val downloadUrl: String?,
        @SerializedName("releaseNotes") val releaseNotes: String?,
    )

    private fun compare(info: VersionInfo): VersionCheckResult {
        val current = parseVersion(currentVersion)
        val latest = parseVersion(info.latestVersion)
        val minRequired = parseVersion(info.minRequiredVersion)

        return when {
            current < minRequired ->
                VersionCheckResult.ForceUpdate(info.downloadUrl, info.releaseNotes)

            current < latest ->
                VersionCheckResult.RecommendUpdate(info.downloadUrl, info.releaseNotes)

            else -> VersionCheckResult.UpToDate
        }
    }

    private fun parseVersion(version: String): Triple<Int, Int, Int> {
        val parts = version.trim().split(".").map { it.toIntOrNull() ?: 0 }
        return Triple(
            parts.getOrElse(0) { 0 },
            parts.getOrElse(1) { 0 },
            parts.getOrElse(2) { 0 },
        )
    }

    private operator fun Triple<Int, Int, Int>.compareTo(other: Triple<Int, Int, Int>): Int {
        if (first != other.first) return first - other.first
        if (second != other.second) return second - other.second
        return third - other.third
    }
}
