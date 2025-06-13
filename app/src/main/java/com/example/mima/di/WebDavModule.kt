package com.example.mima.di

import android.content.Context
import com.example.mima.data.CryptoManager
import com.example.mima.data.DataManager
import com.example.mima.data.HistoryDao
import com.example.mima.data.LoginDao
import com.example.mima.data.RecycleBinDao
import com.example.mima.data.SettingsData
import com.example.mima.data.SyncMetadataDao
import com.example.mima.data.WebDavManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideSettingsData(@ApplicationContext context: Context): SettingsData {
        return SettingsData(context)
    }

    // 你的其他单例提供者，比如 DataManager
    @Provides
    @Singleton
    fun provideDataManager(
        loginDao: LoginDao,
        recycleBinDao: RecycleBinDao,
        historyDao: HistoryDao,
        cryptoManager: CryptoManager,
        webDavManager : WebDavManager,
        settingsData : SettingsData,
        syncMetadataDao: SyncMetadataDao,
    ): DataManager {
        return DataManager(loginDao, recycleBinDao, historyDao, cryptoManager , webDavManager , settingsData , syncMetadataDao)
    }
}