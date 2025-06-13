package com.example.mima.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mima.data.HistoryDao
import com.example.mima.data.LoginDao
import com.example.mima.data.LoginDatabase
import com.example.mima.data.RecycleBinDao
import com.example.mima.data.SyncMetadataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS sync_metadata (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                key TEXT NOT NULL,
                value TEXT NOT NULL
            )
        """.trimIndent())
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LoginDatabase {
        return Room.databaseBuilder(
            context,
            LoginDatabase::class.java,
            "mima-database"
        )
//            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideLoginDao(database: LoginDatabase): LoginDao = database.loginDao()

    @Provides
    fun provideRecycleBinDao(database: LoginDatabase): RecycleBinDao = database.recycleBinDao()

    @Provides
    fun provideHistoryDao(database: LoginDatabase): HistoryDao = database.historyDao()

    @Provides
    fun provideSyncMetadataDao(database : LoginDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }


}
