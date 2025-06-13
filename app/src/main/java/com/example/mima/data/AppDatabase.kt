package com.example.mima.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LoginData::class,
        RecycleBinData::class,
        PasswordHistory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loginDataDao(): LoginDao
    abstract fun recycleBinDao(): RecycleBinDao
    abstract fun historyDao(): HistoryDao
}
