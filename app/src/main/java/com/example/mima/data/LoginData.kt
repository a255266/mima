package com.example.mima.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

// 主数据实体
@Immutable
@Entity(tableName = "login_data")
data class LoginData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectname: String,
    val username: String,
    val password: String,
    val number: String,
    val notes: String,
    val customFieldJson: String, // 存储键值对 JSON 字符串
    val lastModified: Long = System.currentTimeMillis()
)

// 回收站实体
@Entity(tableName = "recycle_bin_data")
data class RecycleBinData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalId: Long,
    val deletedTime: Long = System.currentTimeMillis(),
    val loginDataJson: String,
    val deleteReason: String? = null
)



// 历史记录数据库
@Entity(tableName = "login_history")
data class LoginHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalId: Int,
    val projectName: String,
    val generatedPassword: String,
    val operationType: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 密码历史实体
@Entity(tableName = "password_history")
data class PasswordHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val projectName: String,
    val generatedPassword: String,
    val operationType: String = "GENERATE" // GENERATE/UPDATE
)

//云同步表实体
@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey val id: String = "singleton",
    val lastLocalUpdate: Long = 0L,
    val lastCloudUpdate: Long = 0L,
    val lastSyncTime: Long = 0L,
    val syncStatus: String = "idle"
)

