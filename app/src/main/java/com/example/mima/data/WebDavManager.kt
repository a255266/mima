package com.example.mima.data

import android.content.Context
import android.util.Log
import at.bitfire.dav4jvm.DavResource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import at.bitfire.dav4jvm.ResponseCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.IOException


@Singleton
class WebDavManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore: DataStore<Preferences> = context.webDavDataStore

    private suspend fun loadSettings(): WebDavSettings {
        val prefs = dataStore.data.first()
        return WebDavSettings(
            server = prefs[WebDavKeys.SERVER] ?: "",
            account = prefs[WebDavKeys.ACCOUNT] ?: "",
            password = prefs[WebDavKeys.PASSWORD] ?: ""
        )
    }

    private suspend fun createResource(remotePath: String): DavResource {
        val settings = loadSettings()
        require(settings.server.isNotEmpty() && settings.account.isNotEmpty() && settings.password.isNotEmpty()) {
            "WebDav settings are incomplete"
        }
        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", Credentials.basic(settings.account, settings.password))
                    .build()
                chain.proceed(request)
            }
            .build()
        val cleanPath = remotePath.trimStart('/')
        val fullUrl = "${settings.server.trimEnd('/')}/$cleanPath"
//        Log.d("WebDAV", "fullUrl: $fullUrl")

        try {
            val url = fullUrl.toHttpUrl()
            return DavResource(okHttpClient, url)
        } catch (e: Exception) {
            Log.e("WebDAV", "createResource error: ${e.message}", e)
            throw e
        }
    }


    private suspend fun ensureDirectoryExists(remotePath: String) {
        val resource = createResource(remotePath)

        suspendCancellableCoroutine<Unit> { cont ->
            resource.mkCol(null) { response ->
                if (response.isSuccessful || response.code == 405) {
                    // 405 = Method Not Allowed，表示目录已存在
                    cont.resume(Unit)
                } else {
                    cont.resumeWithException(IOException("创建目录失败: HTTP ${response.code}"))
                }
            }
        }
    }


    suspend fun upload(remotePath: String, file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val resource = createResource(remotePath)
            val parentPath = remotePath.substringBeforeLast('/')
            if (parentPath.isNotEmpty()) {
                try {
                    ensureDirectoryExists(parentPath)
                } catch (e: Exception) {
                    Log.w("WebDAV", "忽略目录创建错误：${e.message}")
                }
            }
            val contentType = "application/octet-stream".toMediaType()
            val requestBody = file.readBytes().toRequestBody(contentType)

            suspendCancellableCoroutine { cont ->
                resource.put(
                    body = requestBody,
                    ifETag = null,
                    ifScheduleTag = null,
                    ifNoneMatch = false,
                    callback = ResponseCallback { response ->
                        if (response.isSuccessful) {
                            cont.resume(true)
                        } else {
                            cont.resumeWithException(IOException("Upload failed: HTTP ${response.code}"))
                        }
                    }
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("WebDAV", "Upload error", e)  // 这里打印详细异常
            false
        }
    }


    suspend fun download(remotePath: String, localFile: File): Boolean = withContext(Dispatchers.IO) {
         try {
            val resource = createResource(remotePath)

            val data = suspendCancellableCoroutine<ByteArray> { cont ->
                resource.get("application/octet-stream", null) { response ->
                    try {
                        val bytes = response.body?.bytes() ?: throw IOException("Empty response body")
                        cont.resume(bytes)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
            }

            localFile.writeBytes(data)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun delete(remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val resource = createResource(remotePath)

            suspendCancellableCoroutine { cont ->
                resource.delete { response ->
                    if (response.isSuccessful) {
                        cont.resume(true)
                    } else {
                        cont.resumeWithException(IOException("Delete failed: HTTP ${response.code}"))
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getLatestBackupFileByName(): Pair<String, Long>? = withContext(Dispatchers.IO) {
        try {
            val backupResource = createResource("backup/")

            suspendCancellableCoroutine<Pair<String, Long>?> { cont ->
                val resultList = mutableListOf<Pair<String, Long>>()
//                Log.d("WebDAV", "开始执行 PROPFIND 请求")
                backupResource.propfind(1) { response , hrefRelation ->
                    val href = response.href ?: return@propfind
                    val name = href.toString().substringAfterLast("/")
//                    Log.d("WebDAV", "发现文件: $name")
                    if (name.startsWith("backup_") && name.endsWith(".json")) {
                        val timestampStr = name.removePrefix("backup_").removeSuffix(".json")
                        val timestamp = timestampStr.toLongOrNull()
//                        Log.d("WebDAV", "尝试解析时间戳: $timestampStr -> $timestamp")
                        if (timestamp != null) {
                            resultList += name to timestamp
//                            Log.d("WebDAV", "匹配到备份文件: $name -> $timestamp")
                        }
                    }
                }

                cont.resume(resultList.maxByOrNull { it.second })
            }
        } catch (e: Exception) {
            Log.e("WebDAV", "获取最新备份失败", e)
            null
        }
    }






}
