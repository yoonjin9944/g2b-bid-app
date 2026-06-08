package com.g2b.bidapp.data.version

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadState {
    data class Progress(val fraction: Float) : DownloadState
    data class Done(val file: File) : DownloadState
    data class Failure(val message: String) : DownloadState
}

@Singleton
class ApkDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    companion object {
        private const val APK_FILE_NAME = "app-update.apk"
    }

    /**
     * APK 를 스트리밍으로 다운로드한다.
     * 진행률(0f ~ 1f)을 [DownloadState.Progress] 로, 완료 시 [DownloadState.Done] 으로 방출한다.
     */
    fun downloadApk(url: String): Flow<DownloadState> = flow {
        try {
            val destFile = getDestinationFile()

            // 이전 다운로드 잔류 파일 제거
            if (destFile.exists()) destFile.delete()

            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Failure("HTTP ${response.code}"))
                    return@flow
                }

                val body = response.body ?: run {
                    emit(DownloadState.Failure("Empty body"))
                    return@flow
                }

                val contentLength = body.contentLength()  // -1 이면 미지원
                var bytesRead = 0L

                val source = body.source()
                val sink = destFile.sink().buffer()
                val buffer = okio.Buffer()
                val chunkSize = 8_192L

                try {
                    while (true) {
                        val read = source.read(buffer, chunkSize)
                        if (read == -1L) break

                        sink.write(buffer, read)
                        // sink.flush() 대신 emitCompleteSegments() — 완성된 세그먼트만 디스크로 내려보냄
                        sink.emitCompleteSegments()

                        bytesRead += read

                        // contentLength == -1 이면 indeterminate(-1f) 방출
                        val fraction = if (contentLength > 0) {
                            bytesRead.toFloat() / contentLength
                        } else {
                            -1f
                        }
                        emit(DownloadState.Progress(fraction))
                    }
                } finally {
                    sink.close()   // close() 가 나머지 버퍼를 flush 하고 파일을 닫는다
                }

                emit(DownloadState.Done(destFile))
            }
        } catch (e: Exception) {
            emit(DownloadState.Failure(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 출처를 알 수 없는 앱 설치 권한 여부를 반환한다.
     */
    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    /**
     * 다운로드된 APK 파일을 설치한다.
     * 설치 권한이 없으면 설정 화면으로 안내하고 false 를 반환한다.
     * 설치 Intent 를 실행했으면 true 를 반환한다.
     */
    fun installApk(apkFile: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(settingsIntent)
                return false
            }
        }

        val authority = "${context.packageName}.fileprovider"
        val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }

    private fun getDestinationFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir   // 외부 저장소 마운트 해제 시 내부 저장소 폴백
        return File(dir, APK_FILE_NAME)
    }
}
