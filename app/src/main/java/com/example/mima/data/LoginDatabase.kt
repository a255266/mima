package com.example.mima.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LoginData::class,
        RecycleBinData::class,
        PasswordHistory::class,
        SyncMetadata::class
    ],
    version = 2,
    exportSchema = false
)

abstract class LoginDatabase : RoomDatabase() {
    abstract fun loginDao(): LoginDao
    abstract fun recycleBinDao(): RecycleBinDao
    abstract fun historyDao(): HistoryDao
    abstract fun syncMetadataDao(): SyncMetadataDao
}