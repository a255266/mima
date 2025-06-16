package com.example.mima.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import androidx.paging.map
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.mima.data.WebDavKeys

private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "MyAppKeyAlias"
private const val DEFAULT_KEY = "Base64OrObfuscatedKeyHere"



@Singleton
class DataManager @Inject constructor(
    private val loginDao: LoginDao,
    private val recycleBinDao: RecycleBinDao,
    private val historyDao: HistoryDao,
    private val cryptoManager: CryptoManager,
    private val webDavManager: WebDavManager,
    private val settingsData: SettingsData,
    private val syncMetadataDao: SyncMetadataDao,
//    private val loginDatabase: LoginDatabase,
) {
    enum class CacheState { INITIAL, LOADING, VALID, INVALID }
    enum class DatabaseType { MAIN, RECYCLE_BIN, HISTORY }
    enum class OperationType {
        GENERATE,  // 密码生成操作
        CREATE,    // 新建记录
        UPDATE,    // 更新记录
        RESTORE,   // 从回收站恢复
        IMPORT     // 数据导入
    }
    // 内部缓存管理
    private val _loginDataCache = mutableStateListOf<LoginData>()
    val loginDataCache: List<LoginData> get() = _loginDataCache
    private val _cacheState = MutableStateFlow(CacheState.INITIAL)
    val cacheState: StateFlow<CacheState> = _cacheState


    @Volatile
    private var skipNextAutoBackup = false

    fun markSkipNextAutoBackup() {
        skipNextAutoBackup = true
    }

    // 数据操作
    suspend fun refreshCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            _cacheState.value = CacheState.LOADING
            val freshData = loginDao.getAll().map { decryptLoginData(it) }
            _loginDataCache.clear()
            _loginDataCache.addAll(freshData)
            _cacheState.value = CacheState.VALID
            true
        } catch (e: Exception) {
            Log.e("DataManager", "刷新缓存失败", e)
            _cacheState.value = CacheState.INVALID
            false
        }
    }

    fun getLoginDataPagingFlow(query: String = ""): Flow<PagingData<LoginData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                loginDao.getAllPagingSource() // 始终取全部
            }
        ).flow
            .map { pagingData ->
                pagingData.map { decryptLoginData(it) }
            }
            .map { decryptedPagingData ->
                if (query.isBlank()) decryptedPagingData
                else decryptedPagingData.filter {
                    it.projectname.contains(query, ignoreCase = true) ||
                            it.username.contains(query, ignoreCase = true) ||
                            it.password.contains(query, ignoreCase = true) ||
                            it.number.contains(query, ignoreCase = true) ||
                            it.notes.contains(query, ignoreCase = true) ||
                            it.customFieldJson.contains(query, ignoreCase = true)
                }
            }
    }



    // TODO:保存数据的函数封装
    suspend fun saveData(
        data: LoginData,
        dbType: DatabaseType,
        operationType: OperationType = OperationType.GENERATE
    ): Long? = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()

        try {
            // 1. 加密敏感数据
//            val encryptedData = if (data.password.isNotBlank()) {
//                data.copy(password = cryptoManager.encryptWithKeyStore(data.password))
//            } else data

            val encryptedData = LoginData(
                id = data.id,
                projectname = cryptoManager.encryptField(data.projectname),
                username = cryptoManager.encryptField(data.username),
                password = cryptoManager.encryptField(data.password),
                number = cryptoManager.encryptField(data.number),
                notes = cryptoManager.encryptField(data.notes),
                customFieldJson = cryptoManager.encryptField(data.customFieldJson),
                lastModified = if (operationType == OperationType.IMPORT) data.lastModified else System.currentTimeMillis()
            )

//            Log.d("SaveData", "保存前数据: $data")
//            Log.d("SaveData", "加密后数据: $encryptedData")

            // 2. 执行数据库操作并获取插入/更新的 ID
            val savedId = when (dbType) {
                DatabaseType.MAIN -> handleMainDatabase(encryptedData, operationType)
                DatabaseType.RECYCLE_BIN -> handleRecycleBin(encryptedData)
                DatabaseType.HISTORY -> handlePasswordHistory(data, operationType)
            }
            val metadata = syncMetadataDao.getMetadata()// 或构造默认值

            if (metadata != null) {
//                Log.e("AutoSync", metadata.toString())
                if (metadata.lastLocalUpdate == 0L) {
                    Log.e("AutoSync", "1")
                    val updated = metadata.copy(
                        lastLocalUpdate = System.currentTimeMillis(),
                        syncStatus = "Uploaded"
                    )
                    syncMetadataDao.upsert(updated)
                }
            }
            // 3. 刷新缓存
            invalidateCache()
//            Log.d("SaveData", "Encryption took: ${System.currentTimeMillis() - start} ms")
            savedId
        } catch (e: Exception) {
//            Log.e("DataManager", "保存到${dbType.name}失败", e)
            null
        }
    }

//批量保存
suspend fun saveAllData(
    dataList: List<LoginData>,
    dbType: DatabaseType,
    operationType: OperationType = OperationType.IMPORT
): Boolean = withContext(Dispatchers.IO) {
    try {
        val encryptedList = dataList.map { data ->
            LoginData(
                id = 0,
                projectname = cryptoManager.encryptField(data.projectname),
                username = cryptoManager.encryptField(data.username),
                password = cryptoManager.encryptField(data.password),
                number = cryptoManager.encryptField(data.number),
                notes = cryptoManager.encryptField(data.notes),
                customFieldJson = cryptoManager.encryptField(data.customFieldJson),
                lastModified = if (operationType == OperationType.IMPORT) data.lastModified else System.currentTimeMillis()
            )
        }

        if (dbType == DatabaseType.MAIN) {
            loginDao.insertAll(encryptedList)
        } else {
            // 你可以扩展支持 recycle bin 或 history 插入
            encryptedList.forEach { saveData(it, dbType, operationType) }
        }

        invalidateCache()
        true
    } catch (e: Exception) {
        Log.e("DataManager", "批量保存失败", e)
        false
    }
}



    // 处理主数据库插入或更新，返回操作后的ID
    private suspend fun handleMainDatabase(data: LoginData, operationType: OperationType): Long {
        val id = if (data.id == 0L) {
            loginDao.insert(data)  // 返回插入新ID
        } else {
            loginDao.update(data)
            data.id
        }

        // 记录密码变更历史（独立事务或可改为外部调用）
        if (data.password.isNotBlank()) {
            historyDao.insert(
                PasswordHistory(
                    projectName = data.projectname,
                    generatedPassword = data.password,
                    operationType = operationType.name
                )
            )
        }
        return id
    }

    // 处理回收站插入，返回插入ID
    private suspend fun handleRecycleBin(data: LoginData): Long {
        return recycleBinDao.insert(
            RecycleBinData(
                originalId = data.id,
                loginDataJson = Gson().toJson(data)
            )
        )
    }

    // 处理密码历史插入，返回插入ID或0（如果密码为空）
    private suspend fun handlePasswordHistory(data: LoginData, operationType: OperationType): Long {
        return if (data.password.isNotBlank()) {
            historyDao.insert(
                PasswordHistory(
                    projectName = data.projectname,
                    generatedPassword = data.password,
                    operationType = operationType.name
                )
            )
        } else 0L
    }

    suspend fun moveToRecycleBin(data: LoginData, reason: String? = null): Boolean {
        return try {
            saveData(data, DatabaseType.RECYCLE_BIN)
            loginDao.delete(data)
            invalidateCache()
            true
        } catch (e: Exception) {
            Log.e("DataManager", "移动到回收站失败", e)
            false
        }
    }



    // 数据流操作
    fun getAllLoginDataFlow(): Flow<List<LoginData>> {
        return loginDao.getAllFlow()
            .map { list ->
                list.map { decryptLoginData(it) }.also { decryptedList ->
                    _loginDataCache.clear()
                    _loginDataCache.addAll(decryptedList)
                    _cacheState.value = CacheState.VALID
                }
            }
    }

    // 查询方法
    suspend fun getMainDataById(id: Long): LoginData? {
        return loginDao.getById(id)?.let { decryptLoginData(it) }
    }

    suspend fun getAllMainData(): List<LoginData> {
        return loginDao.getAll().map { decryptLoginData(it) }
    }

    suspend fun getAllRecycleData(): List<RecycleBinData> {
        return recycleBinDao.getAll()
    }

    suspend fun getPasswordHistory(projectName: String? = null): List<PasswordHistory> {
        return if (projectName != null) {
            historyDao.getByProject(projectName)
        } else {
            historyDao.getAll()
        }
    }

    // TODO:导入导出
    suspend fun exportDataAsEncryptedString(password: String?): String? = withContext(Dispatchers.IO) {
        try {
            val data = getAllMainData()
            val json = Gson().toJson(data)

            val actualPassword = password ?: DEFAULT_KEY
            Log.d("AutoSync", "使用了$actualPassword")
            val encryptedData = cryptoManager.encryptWithPassword(json, actualPassword)

            val exportWrapper = mapOf(
                "version" to 2,
                "mode" to if (password != null) "user_key" else "fixed_key",
                "encryptedData" to encryptedData
            )

            Gson().toJson(exportWrapper)
        } catch (e: Exception) {
            Log.e("AutoSync", "导出数据失败", e)
            null
        }
    }

    suspend fun importDataWithKey(remotePath: Long?,password: String?, input: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 尝试解析外层结构
            val jsonObject = JsonParser.parseString(input).asJsonObject
            val mode = jsonObject.get("mode")?.asString ?: "fixed_key"
            val encrypted = jsonObject.get("encryptedData")?.asString
                ?: throw IllegalArgumentException("数据格式不正确")

            val actualPassword = if (mode == "user_key") {
                password ?: throw IllegalArgumentException("需要主密码")
            } else {
                DEFAULT_KEY
            }
            Log.d("AutoSync", "导入时间戳: $remotePath")
            val decrypted = cryptoManager.decryptWithPassword(encrypted, actualPassword)
            val data = Gson().fromJson(decrypted, Array<LoginData>::class.java).toList()
            Log.d("AutoSync", "导入数据原文: $input")

            // 清空现有数据
            loginDao.getAll().forEach { loginData ->
                loginDao.delete(loginData)
            }

            val newDataList = data.map { it.copy(id = 0) }

            // 批量加密+插入方法
            val success = saveAllData(newDataList, DatabaseType.MAIN, OperationType.IMPORT)

            // 写入同步元数据（导入时间戳）
            val metadata = SyncMetadata(
                id = "singleton",
                lastLocalUpdate = remotePath ?: System.currentTimeMillis(),
            )
            markSkipNextAutoBackup()
            syncMetadataDao.upsert(metadata)

            // 刷新缓存
            refreshCache()

            success
        } catch (e: Exception) {
            Log.e("AutoSync", "导入数据失败，原始数据: $input", e)
            throw e
        }
    }


    // TODO: 云端同步 - 上传数据（加密后）

    suspend fun uploadEncryptedDataToCloud(remotePath: String, password: String?): Boolean {
        SyncStatusBus.update("检测到数据变更，同步中", SyncStatusType.Info)
        Log.d("AutoSync", "上传数据")
        val encryptedJson = exportDataAsEncryptedString(password) ?: return false

        val file = File.createTempFile("backup", ".json").apply {
            writeText(encryptedJson, Charsets.UTF_8)
        }

        return try {
            val success = webDavManager.upload(remotePath, file)
            if (success) {
                remotePath
                    .substringAfterLast("backup_")
                    .substringBeforeLast(".json")
                    .toLongOrNull()
                    ?.let {
                        SyncStatusBus.update("上传成功 ${formatTimestamp(it)}", SyncStatusType.Success)
                    }
                Log.d("AutoSync", "上传成功: $remotePath")
            } else {
                SyncStatusBus.update("上传失败", SyncStatusType.Error)
                Log.e("AutoSync", "上传失败")
            }
            success
        } catch (e: Exception) {
            Log.e("AutoSync", "上传异常", e)
            false
        } finally {
            file.delete() // 上传完删除临时文件
        }
    }


    suspend fun downloadAndImportFromCloud(remotePath: Long?, password: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            SyncStatusBus.update("正在下载云端数据…", SyncStatusType.Info)

            val tempFile = File.createTempFile("import", ".json")
            val fileName = "backup/backup_${remotePath}.json"
            val success = webDavManager.download(fileName, tempFile)
            if (!success) {
                Log.e("AutoSync", "下载失败: $fileName")
                SyncStatusBus.update("云端数据下载失败", SyncStatusType.Error)
                tempFile.delete()
                return@withContext false
            }

            SyncStatusBus.update("下载成功，正在导入数据…", SyncStatusType.Info)

            val content = tempFile.readText(Charsets.UTF_8)
            tempFile.delete()
            Log.e("AutoSync", "云端时间戳: $remotePath")
            val importResult = importDataWithKey(remotePath,password, content)
            Log.d("AutoSync", "导入结果: $importResult")
            if (importResult) {
                SyncStatusBus.update("数据导入成功", SyncStatusType.Success)
            } else {
                SyncStatusBus.update("数据导入失败", SyncStatusType.Error)
            }
            importResult
        } catch (e: Exception) {
            Log.e("AutoSync", "导入失败", e)
            SyncStatusBus.update("下载导入异常: ${e.message}", SyncStatusType.Error)
            false
        }
    }


    fun startAutoBackupOnDatabaseChange(lifecycleScope: CoroutineScope) {
        Log.d("AutoSync", "调用了startAutoBackupOnDatabaseChange")
        lifecycleScope.launch {
            var isFirstEmission = true
            Log.d("AutoSync", "自动备份")
            loginDao.getAllFlow()
                .debounce(2000) // 防止频繁变动（例如用户快速连续输入）
                .distinctUntilChanged()
                .collectLatest { loginList ->
                    val a111 = loginList.isNotEmpty()
                    Log.d("AutoSync", "Flow 触发，当前数据：$a111")
                    if (isFirstEmission) {
                        Log.d("AutoSync", "跳过首次发射")
                        isFirstEmission = false
                        return@collectLatest // 跳过首次发射
                    }
                    if (skipNextAutoBackup) {
                        skipNextAutoBackup = false
                        Log.d("AutoSync", "跳过这次自动备份（导入产生数据变更）")
                        return@collectLatest
                    }
                    Log.d("AutoSync", "loginList 是否非空: ${loginList.isNotEmpty()}")

                    if (loginList.isNotEmpty()) {
                        try {
                            val metadata = syncMetadataDao.getMetadata() // 默认 ID 是 "singleton"
                            metadata?.syncStatus?.let { Log.d("AutoSync", "syncStatus$it") }
                            val key = settingsData.decryptKey.firstOrNull().takeUnless { it.isNullOrBlank() } ?: DEFAULT_KEY
                            Log.d("AutoSync", "Flow，秘钥：$key")
                                val timestamp = System.currentTimeMillis()//.toString()
                                val fileName = "backup/backup_$timestamp.json"


                                uploadEncryptedDataToCloud(fileName, key)
                                // 可选：更新同步元数据
                                val updatedMetadata  = SyncMetadata(
                                    id = "singleton",
                                    lastLocalUpdate = timestamp,
                                    lastCloudUpdate = timestamp,
                                    lastSyncTime = timestamp,
                                    syncStatus = "Success"
                                )
                                syncMetadataDao.upsert(updatedMetadata )
                        } catch (e: Exception) {
                            Log.e("AutoSync", "自动备份失败: ${e.message}")
                        }
                    }
                }
        }
    }


    suspend fun ensureMetadataExists() {
        val exists = syncMetadataDao.getMetadata() != null
        if (!exists) {
            syncMetadataDao.upsert(SyncMetadata())
        }
    }



    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }

    // TODO:同步逻辑判断
    suspend fun performSyncIfNeeded() = withContext(Dispatchers.IO) {

        ensureMetadataExists()
        Log.d("AutoSync", "正在检测本地与云端数据是否一致")
        SyncStatusBus.update("正在检测本地与云端数据是否一致", SyncStatusType.Info)
        val metadata = syncMetadataDao.getMetadata() ?: return@withContext
        val cloudTimestamp = webDavManager.getLatestBackupFileByName()?.second
        val key = settingsData.decryptKey.firstOrNull().takeUnless { it.isNullOrBlank() } ?: DEFAULT_KEY
        Log.d("AutoSync", "秘钥为：$key")
        Log.d("AutoSync", "云端数据为：$cloudTimestamp 本地数据为 $metadata.lastLocalUpdate")
        when {

            cloudTimestamp == null && metadata.lastLocalUpdate != 0L -> {
                SyncStatusBus.update(
                    "本地数据较新:${formatTimestamp(metadata.lastLocalUpdate)}，正在上传数据",
                    SyncStatusType.Info
                )

                // 云端为null上传
//                val key = settingsData.decryptKey.firstOrNull()

//                if (!key.isNullOrBlank()) {
                    val timestamp = metadata.lastLocalUpdate
                    val fileName = "backup/backup_$timestamp.json"
                    val success = uploadEncryptedDataToCloud(fileName, key)
                    if (success) {
                        syncMetadataDao.upsert(
                            metadata.copy(
                                lastCloudUpdate = timestamp,
                                lastSyncTime = System.currentTimeMillis(),
                                syncStatus = "Uploaded"
                            )
                        )
                        SyncStatusBus.update("上传成功", SyncStatusType.Success)
                    } else {
                        SyncStatusBus.update("上传失败", SyncStatusType.Error)
                    }
                    Log.d("AutoSync", "云端为null且本地不为0L上传")
//                }
            }

            cloudTimestamp != null && metadata.lastLocalUpdate > cloudTimestamp -> {
                SyncStatusBus.update(
                    "本地数据较新:${formatTimestamp(metadata.lastLocalUpdate)}，正在上传数据",
                    SyncStatusType.Info
                )
                // 本地比云端新 → 上传
//                val key = settingsData.decryptKey.firstOrNull()
//                if (!key.isNullOrBlank()) {
                    val timestamp = metadata.lastLocalUpdate
                    val fileName = "backup/backup_$timestamp.json"
                    val success = uploadEncryptedDataToCloud(fileName, key)
                    if (success) {
                        syncMetadataDao.upsert(
                            metadata.copy(
                                lastCloudUpdate = timestamp,
                                lastSyncTime = System.currentTimeMillis(),
                                syncStatus = "Uploaded"
                            )
                        )
                        SyncStatusBus.update("上传成功", SyncStatusType.Success)
                    } else {
                        SyncStatusBus.update("上传失败", SyncStatusType.Error)
                    }
//                }
                Log.d("AutoSync", "上传")
            }

            cloudTimestamp != null && cloudTimestamp > metadata.lastLocalUpdate -> {
                SyncStatusBus.update(
                    "云端数据较新:${formatTimestamp(cloudTimestamp)}，正在下载导入数据",
                    SyncStatusType.Info
                )
                // 云端比本地新 → 下载并导入
                Log.d("AutoSync", "云端比本地新 → 下载并导入")
//                val key = settingsData.decryptKey.firstOrNull()
//                if (!key.isNullOrBlank()) {
//                    val fileName = "backup/backup_${cloudTimestamp}.json"
//                    val success = downloadAndImportFromCloud(fileName, key)
                val success = downloadAndImportFromCloud(cloudTimestamp, key)
                    Log.d("AutoSync", "success")
                    if (success) {
//                        syncMetadataDao.upsert(
//                            metadata.copy(
//                                lastLocalUpdate = metadata.lastCloudUpdate,
//                                lastSyncTime = System.currentTimeMillis(),
//                                syncStatus = "Downloaded"
//                            )
//                        )
                        SyncStatusBus.update("导入成功", SyncStatusType.Success)
                    }
//                }
            }

            cloudTimestamp == null -> {
                SyncStatusBus.update("云端数据获取失败", SyncStatusType.Error)
            }

            else -> {
                // 时间戳一致，无需同步
                SyncStatusBus.update(
                    "本地和云端数据一致，版本:${formatTimestamp(metadata.lastLocalUpdate)}",
                    SyncStatusType.Success
                )
                Log.d("AutoSync", "本地和云端数据一致，无需同步")
            }
        }
    }




    // 解密方法
    private fun decryptLoginData(data: LoginData): LoginData {
        return try {
            data.copy(
                projectname = cryptoManager.decryptField(data.projectname),
                username = cryptoManager.decryptField(data.username),
                password = cryptoManager.decryptField(data.password),
                number = cryptoManager.decryptField(data.number),
                notes = cryptoManager.decryptField(data.notes),
                customFieldJson = cryptoManager.decryptField(data.customFieldJson)
            )
        } catch (e: Exception) {
//            Log.e("DataManager", "解密登录数据失败", e)
            data.copy(password = "解密失败")
        }
    }


    suspend fun getProjectIdByName(projectName: String): Long? {
        return withContext(Dispatchers.IO) {
            loginDao.getProjectIdByName(projectName)
        }
    }


    private fun invalidateCache() {
        _cacheState.value = CacheState.INVALID
    }

    fun getCachedLoginData(): List<LoginData> = loginDataCache.toList()

    suspend fun createData(data: LoginData): Long? =
        saveData(data, DatabaseType.MAIN, OperationType.CREATE)

    suspend fun updateData(data: LoginData): Long? =
        saveData(data, DatabaseType.MAIN, OperationType.UPDATE)

    suspend fun saveOrUpdate(data: LoginData): Long? {
        return if (data.id == 0L) createData(data) else updateData(data)
    }



}