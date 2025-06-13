package com.example.mima.di

import android.content.Context
import com.example.mima.data.CryptoManager
import com.example.mima.data.WebDavManager
import com.example.mima.util.SoundHelper
import com.example.mima.util.VibrationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSoundHelper(@ApplicationContext context: Context): SoundHelper {
        return SoundHelper(context)
    }

    @Provides
    @Singleton
    fun provideWebDavManager(@ApplicationContext context: Context): WebDavManager {
        return WebDavManager(context)
    }
}
